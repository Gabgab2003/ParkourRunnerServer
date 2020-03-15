package net.downloadpizza.prserver.types

import com.beust.klaxon.Json

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

data class ParkGeometry(
    val type: String,
    val coordinates: List<Double>
)

data class ParkProperties(
    @Json(name="OBJECTID")
    val objectId: Int,
    @Json(name="ANL_NAME")
    val institutionName: String?,
    @Json(name="BEZIRK")
    val district: Int?,
    @Json(name="FLAECHE")
    val area: String?,
    @Json(name="OEFF_ZEITEN")
    val openingHours: String?,
    @Json(name="SPIELEN_IM_PARK")
    val playingInPark: String?,
    @Json(name="WASSER_IM_PARK")
    val waterInPark: String?,
    @Json(name="HUNDE_IM_PARK")
    val dogsInPark: String?,
    @Json(name="TELEFON")
    val telephone: String?,
    @Json(name="WEBLINK1")
    val weblink: String?,
    @Json(name="SE_ANNO_CAD_DATA")
    val cadData: String?
)

interface IPark {
    val type: String
    val id: String
    val geometry: ParkGeometry
    val geometryName: String
    val properties: ParkProperties
}

data class Park(
    override val type: String,
    override val id: String,
    override val geometry: ParkGeometry,
    @Json(name = "geometry_name")
    override val geometryName: String,
    override val properties: ParkProperties
): IPark

data class ParkWithInfo(
    override val type: String,
    override val id: String,
    override val geometry: ParkGeometry,
    @Json(name = "geometry_name")
    override val geometryName: String,
    override val properties: ParkProperties,
    val distance: Double,
    val bearing: Double

): IPark

data class ParkFile(
    val type: String,
    val totalFeatures: Int,
    val features: List<Park>
)