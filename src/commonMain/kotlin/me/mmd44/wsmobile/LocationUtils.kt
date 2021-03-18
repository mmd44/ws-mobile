package me.mmd44.wsmobile

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 *
 *  A helper class for handling location items.
 *
 */

@Serializable
data class LocationEvent(val lat: Double, val lon: Double, val time: Long, val ext: String?)

data class LocationCoordinates (val latitude: Double, val longitude: Double)

fun buildLocationDataItemJSON(
    locationCoordinates: LocationCoordinates,
    timestamp: Long?,
    extra: String?
): String {
    val time = timestamp ?: Clock.System.now().epochSeconds

    val locationEvent = LocationEvent(
        locationCoordinates.latitude,
        locationCoordinates.longitude,
        time,
        extra
    )
    return Json.encodeToString(locationEvent)
}