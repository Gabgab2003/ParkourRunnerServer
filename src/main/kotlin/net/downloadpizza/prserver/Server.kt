package net.downloadpizza.prserver

import com.beust.klaxon.*
import net.downloadpizza.prserver.types.GeoPosition
import org.http4k.core.*
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import java.lang.Exception
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
                try {
                    val parks = database.getParks(pos.coords, limit)
                    Response(OK).body(klaxon.toJsonString(parks))
                } catch (e: Exception) {
                    println(e.stackTrace)
                    Response(OK)
                }
                }
        }
    )

    val server = app.asServer(Jetty(8000)).start()

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            server.stop()
        }
    })
}