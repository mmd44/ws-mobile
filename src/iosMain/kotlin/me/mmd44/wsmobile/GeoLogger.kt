package me.mmd44.wsmobile

import kotlinx.cinterop.useContents
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.mmd44.wsmobile.GeoLogger.Companion.TAG
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.UIKit.UIDevice
import platform.darwin.NSObject

/**
 * A KMM module built on top of Google fused location provider for Android and CLLocationManager for iOS.
 *
 * Sends an HTTP post request to a pre-defined server containing the user's current location info upon calling the log() function.
 *
 * Asking for permissions should be handled explicitly by developers.
 *
 * Uses an asynchronous mechanism and no errors should be thrown upon using this module; all errors are handled internally and logged.
 * The output must be monitored in logs for any failure or success (HTTP response) using the tag [TAG].
 *
 *
 * @suppress For a more practical solution a callback might be added so to handle the final output; but I considered this
 * @suppress to be a silent location provider as I understood from the requirements!
 *
 */
actual class GeoLogger actual constructor() {

    private lateinit var locationManager: CLLocationManager

    private var timestamp: Long? = 0
    private var extra: String? = null


    /**
     * Setup before the log call.
     *
     * This is called *internally* only and not by end users.
     *
     * @return true if setup is successful; false means that the location provider is turned off from settings or setup fails for some weird reasons.
     *
     */
    internal actual fun setup(): Boolean {

        // The accuracy in iOS differs from Android as it will have no effect
        // if kCLLocationAccuracyBest is requested but no permission is granted;
        // it will automatically fall to a lower accuracy as docs state it.
        // So we will just print a warn message for iOS 14+ as developers will handle asking for permissions.
        return try {
            locationManager = CLLocationManager().apply {
                delegate = GeoLocatorDelegateProtocol()
                desiredAccuracy = kCLLocationAccuracyBest
            }
            if (locationManager.locationServicesEnabled()) true
            else {
                log("$TAG $this ${LocationNotEnabledException().message}")
                false
            }
        } catch (e: Exception) {
            log("$TAG $this ${LocationSetupException().message}")
            false
        }
    }

    /**
     * Gets the current location and posts a JSON body to https://httpbin.org
     * with a required ApplicationContext [context] on Android / NSObject on iOS, an optional Long [timestamp], and a String [extra] string.
     *
     * Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission;
     * But, it will not throw an exception if called without providing any of these permissions.
     *
     * For a better accuracy provide ACCESS_FINE_LOCATION.
     *
     * If the [timestamp] is passed as null, it will be generated at post time.
     *
     * If the [extra] is passed as null, it will remain null in the JSON body.
     *
     *
     * @suppress The logic is implemented with Tasks and Callbacks for getting the location and COROUTINES are used only when making the POST request;
     * @suppress although a more elegant solution would have been by implementing all the logic using coroutines, I made this decision to make it as consistent as
     * @suppress possible with the native (iOS) implementation on which coroutines are still in an experimental phase.
     *
     *
     */
    actual fun log(context: ApplicationContext, timestamp: Long?, extra: String?) {
        if (setup()) {
            this.timestamp = timestamp
            this.extra = extra

            if (isLocationPermissionGranted()) {
                locationManager.startUpdatingLocation()
            } else  {
                postToRemoteServerWithZeroes(extra = extra, timestamp = timestamp)
                log("$TAG $this ${LocationPermissionException().message}")
            }
        }
    }


    /**
     *
     *
     * Delegate protocol to handle location updates as required by native iOS
     *
     *
     */
    inner class GeoLocatorDelegateProtocol() : NSObject(), CLLocationManagerDelegateProtocol {

        private lateinit var locationCoordinates: LocationCoordinates

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val locations = didUpdateLocations as List<CLLocation>

            // This is a new authorization added for iOS 14+
            if (UIDevice.currentDevice.systemVersion.startsWith("14") /* Need a better solution as String.toDouble doesn't work with native code */) {
                if (manager.accuracyAuthorization != CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy) {
                    log("$TAG $this Warning: Please allow use for precise location for better accuracy")
                }
            }

            // This guarantees a single update for each log function call
            if (!this::locationCoordinates.isInitialized) {
                manager.stopUpdatingLocation()
                locationCoordinates = locations.last().coordinate().useContents {
                    LocationCoordinates(
                        latitude = latitude,
                        longitude = longitude
                    )
                }
                postToRemoteServer (locationCoordinates, timestamp, extra)
            }

        }

        // When it fails to get a location update
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            log("$TAG $this Error $didFailWithError")
        }
    }


    /**
     *
     * Checks if a location permissions is available.
     *
     */
    private fun isLocationPermissionGranted(): Boolean {
        return listOf(
            kCLAuthorizationStatusAuthorized,
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse,
        ).contains(locationManager.authorizationStatus())
    }

    /**
     *
     * Launches the coroutine for posting the location
     *
     */
    private fun postToRemoteServer (locationCoordinates: LocationCoordinates, timestamp: Long?, extra: String?) {
        log("$TAG $this Your location: LAT -> ${locationCoordinates.latitude}, LON -> ${locationCoordinates.longitude}")
        GlobalScope.launch(IOSDispatcher) {
            RemoteLogger(null, locationCoordinates, timestamp, extra).postLocationEvent()
        }
    }

    /**
     *
     * Launches the coroutine for posting the location with (0,0) coordinates
     *
     */
    private fun postToRemoteServerWithZeroes(timestamp: Long?, extra: String?) {
        postToRemoteServer(LocationCoordinates(0.0, 0.0), timestamp, extra)
    }

    companion object {
        private const val TAG = "ws-geo-log-lib"
    }
}

