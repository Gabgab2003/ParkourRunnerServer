package net.downloadpizza.prserver

import com.beust.klaxon.*
import net.downloadpizza.prserver.types.GeoPosition
import org.http4k.core.*
import org.http4k.core.Method.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import java.security.MessageDigest

val klaxon = Klaxon()

const val DEFAULT_LIMIT = 10

private fun hashString(input: String): ByteArray = MessageDigest
    .getInstance("SHA-256")
    .digest(input.toByteArray())

const val jdbc = "jdbc:mysql://localhost:3306/parkourdata"
const val driver = "com.mysql.cj.jdbc.Driver"

fun main() {
    val database = DbConnector(Database.connect(jdbc, driver, "root", "sqlpassword"))

    val app: HttpHandler = routes(
        "getparks" bind POST to { req ->
            println(req.bodyString())
            val pos = klaxon.parse<GeoPosition>(req.bodyString())
            if (pos == null)
                Response(BAD_REQUEST)
            else {
                val limit = req.query("limit")?.toIntOrNull() ?: DEFAULT_LIMIT
                val parks = database.getParksByDistance(pos.coords, limit)
                Response(OK).body(klaxon.toJsonString(parks))
            }
        },
        "getuser/{id}" bind GET to { req ->
            val id = req.path("id")
            if (id != null) {
                val user = database.getUser(id)
                Response(OK).body(klaxon.toJsonString(toJsonUser(user)))
            } else {
                Response(BAD_REQUEST).body("No user id given")
            }

        },
        "getpark/{id}" bind GET to { req ->
            val id = req.path("id")
            if (id != null) {
                val user = database.getPark(id)
                Response(OK).body(klaxon.toJsonString(toJsonPark(user)))
            } else {
                Response(BAD_REQUEST).body("No park id given")
            }
        },
        "gettop/{n}" bind GET to { req ->
            val n = req.path("n")?.toInt() ?: 10
            Response(OK).body(klaxon.toJsonString(database.getTopN(n)))
        },
        "getplacement/{id}" bind GET to { req ->
            val id = req.path("id")
            if (id != null) {
                val placement = database.getPlacement(id)
                Response(OK).body(placement.toString())
            } else {
                Response(BAD_REQUEST).body("No park id given")
            }
        }
    )

//    val debuggedApp = DebuggingFilters.PrintRequestAndResponse().then(app)
//    val server = debuggedApp.asServer(Jetty(8000)).start()

    val server = app.asServer(Jetty(8000)).start()
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            server.stop()
        }
    })
}