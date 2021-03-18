package me.mmd44.wsmobile

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 *
 *  Commonly used to post the location data using coroutines.
 *
 */
class RemoteLogger constructor(
    private val httpClient: HttpClient?,
    private val locationCoordinates: LocationCoordinates,
    private val timestamp: Long?,
    private val extra: String?
) {

    private val defaultHttpClient: HttpClient = httpClient ?: HttpClient() {
        expectSuccess = false
    }

    suspend fun postLocationEvent() {

        val response = try {
            defaultHttpClient.post<HttpResponse>("https://httpbin.org/post") {
                headers {
                    append("Accept", "application/json")
                }
                body = buildLocationDataItemJSON(locationCoordinates, timestamp, extra)
            }
        } catch (e: Exception) {
            log("$TAG Exception: HTTP Post Failure")
            null
        }

        if (response?.status == HttpStatusCode.OK) {
            log("$TAG HTTP status success: ${response.status.description} --- bodyText: ${response.readText()}")
        }

        defaultHttpClient.close()
    }

    companion object {
        private const val TAG = "ws-geo-log-lib"
    }
}