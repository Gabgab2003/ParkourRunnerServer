package net.downloadpizza.prserver

import net.downloadpizza.prserver.types.Coordinates
import net.downloadpizza.prserver.types.SimpleCoordinates
import net.downloadpizza.prserver.types.bearingBetween
import net.downloadpizza.prserver.types.distanceBetween
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SortOrder.*
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant

/*
Table l_log as L {
  l_u_user varchar(24) [ref: > U.u_name]
  l_p_park varchar(45) [ref: > P.p_id]
  l_timestamp timestamp
}

Table u_users as U {
  u_name varchar(24) [pk]
  u_pwhash char()
}

Table p_parks as P {
  p_id varchar(45)
  p_location point
  p_name varchar(60)
}
 */

object Log : IntIdTable("l_logs", "l_id") {
    val user = reference("l_u_user", Users.id)
    val park = reference("l_p_park", Parks.id)
    val time = timestamp("l_timestamp")
}

object Users : IdTable<String>("u_users") {
    override val id = varchar("u_id", 24).entityId()
    val pwhash = binary("u_pwhash", 32)
    val points = long("u_points")
}

object Parks : IdTable<String>("p_parks") {
    override val id = varchar("p_id", 45).entityId()

    val name = varchar("p_name", 60)

    val longitude = double("p_longitude")
    val latitude = double("p_latitude")
}

class LogEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LogEntry>(Log)

    var user by Log.user
    var park by Log.park
    var time by Log.time
}

class User(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, User>(Users)

    var pwhash by Users.pwhash
    var points by Users.points
}

class Park(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Park>(Parks)

    var longitude by Parks.longitude
    var latitude by Parks.latitude

    var name by Parks.name

    val coords get() = SimpleCoordinates(this.latitude, this.longitude)
}

const val RECAP_LIMIT = 4

class DbConnector(private val db: Database) {

    fun getParksByDistance(coords: Coordinates, limit: Int? = null) = transaction(db) {
        Park.all().map { toParkWithInfo(it, distanceBetween(coords, it.coords), bearingBetween(coords, it.coords)) }
            .sortedBy {
                it.distance
            }.run { if (limit != null) this.take(limit) else this }
    }

    fun visitPark(user: User, park: Park): Boolean = transaction(db) {
        val now = Instant.now()
        val legal = !LogEntry
            .find { (Log.user eq user.id) and (Log.park eq park.id) }
            .orderBy(Pair(Log.time, DESC)).limit(1)
            .any { Duration.between(it.time, now).toHours() < RECAP_LIMIT }
        if (legal) {
            LogEntry.new {
                this.user = user.id
                this.park = park.id
                this.time = now
            }

            user.points += 1
            commit()
        }
        legal
    }

    fun getUser(id: String) = transaction(db) { User[id] }
    fun getPark(id: String) = transaction(db) { Park[id] }

    fun getTopN(n: Int) = transaction(db) {
        User.all().orderBy(Pair(Users.points, DESC)).limit(n).map(::toJsonUser)
    }

    fun getPlacement(id: String) = transaction(db) {
        User.all().orderBy(Pair(Users.points, DESC)).indexOf(User[id])
    }

    fun createUser(id: String, pwhash: ByteArray) = transaction(db) {
        if (User.findById(id) != null) {
            false
        } else {
            User.new(id) {
                this.pwhash = pwhash
                this.points = 0
            }
            commit()
            true
        }
    }

    fun changePassword(id: String, pwhash: ByteArray) = transaction(db) {
        if (User.findById(id) == null) {
            false
        } else {
            User[id].pwhash = pwhash
            commit()
            true
        }
    }
}

data class ParkWithInfo(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val name: String,
    val distance: Double,
    val bearing: Double
)

fun toParkWithInfo(park: Park, distance: Double, bearing: Double) = ParkWithInfo(
    park.id.value,
    park.longitude,
    park.latitude,
    park.name,
    distance,
    bearing
)


data class JsonPark(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val name: String
)

fun toJsonPark(park: Park) = JsonPark(
    park.id.value,
    park.longitude,
    park.latitude,
    park.name
)


data class JsonUser(
    val id: String,
    val points: Long
)

fun toJsonUser(user: User) = JsonUser(
    user.id.value,
    user.points
)

data class JsonLogEntry(
    val user: String,
    val park: String,
    val time: Instant
)

fun toJsonLogEntry(logEntry: LogEntry) = JsonLogEntry(
    logEntry.user.value,
    logEntry.park.value,
    logEntry.time
)