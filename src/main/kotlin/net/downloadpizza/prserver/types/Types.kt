package net.downloadpizza.prserver.types

import kotlin.math.*

interface Coordinates {
    val latitude: Double
    val longitude: Double

    operator fun component1() = latitude
    operator fun component2() = longitude
}

data class SimpleCoordinates(
    override val latitude: Double,
    override val longitude: Double
): Coordinates

data class GeoCoordinates(
    override val latitude: Double,
    override val longitude: Double,
    val accuracy: Double,
    val altitude: Double,
    val heading: Double,
    val speed: Double
): Coordinates

data class GeoPosition(
    val coords: GeoCoordinates,
    val timestamp: Double,
    val mocked: Boolean? = null
)

private fun Double.toRadians() = PI * (this / 180)
private infix fun Double.delta(other: Double) = abs(this - other)

const val earthRadiusM: Double = 63781*1e6

fun distanceBetween(from: Coordinates, to: Coordinates): Double {
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val originLat = Math.toRadians(from.latitude)
    val destinationLat = Math.toRadians(to.latitude)

    val a = sin(dLat / 2).pow(2.toDouble()) + sin(dLon / 2).pow(2.toDouble()) * cos(originLat) * cos(destinationLat)
    val c = 2 * asin(sqrt(a))
    return earthRadiusM * c
}

fun bearingBetween(cord1: Coordinates, cord2: Coordinates): Double {
    val lon1 = cord1.longitude.toRadians()
    val lat1 = cord1.latitude.toRadians()

    val lon2 = cord2.longitude.toRadians()
    val lat2 = cord2.latitude.toRadians()

    // L = long | θ = lat

    val deltaLong = lon1 delta lon2

    // X = cos θb * sin ∆L
    val x = cos(lat2) * sin(deltaLong)

    // Y = cos θa * sin θb – sin θa * cos θb * cos ∆L
    val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLong)

    // β = atan2(X,Y)
    return atan2(x, y)
}