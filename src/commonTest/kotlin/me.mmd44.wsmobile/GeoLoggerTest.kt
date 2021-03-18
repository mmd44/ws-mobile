package me.mmd44.wsmobile

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertTrue


/**
 *
 * Simple unit tests for the setup and the common RemoteLogger.
 *
 * As for end to end tests, it is better to test the module as part of a dependency in each platform
 * as the current implementation has only logs as output and KMM has little support for the available testing libraries such as mockito and espresso.
 *
 * One way for testing this module as an end to end test could be by "mocking" the logger and checking the logs; but this is not done here!
 *
 */
@ExperimentalCoroutinesApi
class GeoLoggerTest {

    @Test
    fun testSetup() {
        val geoLogger = GeoLogger ()
        assertTrue(geoLogger.setup(), "Setup should return 'true'")
    }

    @Test
    fun remoteLogger_postToHttpBin () {
        val customEngine = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (val urlString = "https://${request.url.host}${request.url.fullPath}") {
                        "https://httpbin.org/post" -> {
                            val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            val body = buildLocationDataItemJSON(LocationCoordinates(35.12345, 36.6543321), 123456789, null)
                            respond(body, headers = responseHeaders)
                        }
                        else -> error("Unhandled $urlString")
                    }
                }
            }
        }

        val remoteLogger = RemoteLogger (customEngine, LocationCoordinates(35.12345, 36.6543321), null, null)

        runBlockingTest() {
            remoteLogger.postLocationEvent()
        }
    }

}