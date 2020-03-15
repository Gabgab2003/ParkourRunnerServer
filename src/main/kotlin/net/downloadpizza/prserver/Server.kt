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
import java.security.MessageDigest

val klaxon = Klaxon()

const val DEFAULT_LIMIT = 10

private fun hashString(input: String, algorithm: String = "SHA-256"): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })
}

fun main() {
    val parkStore = ParkStore(object {}.javaClass.getResourceAsStream("/PARKINFOOGD.json"))

    val app: HttpHandler = routes(
        "getparks" bind POST to { req ->
            println(req.bodyString())
            val pos = klaxon.parse<GeoPosition>(req.bodyString())
            if (pos == null) Response(BAD_REQUEST)
            else {
                val limit = req.query("limit")?.toIntOrNull() ?: DEFAULT_LIMIT
                val parks = parkStore.sortedByDistance(pos.coords).take(limit)
                println(klaxon.toJsonString(parks))
                Response(OK).body(klaxon.toJsonString(parks))
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