package me.mmd44.wsmobile

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


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

    // The LocationRequest specifying the conditions to be met.
    private var priority: Int = -1

    // The FusedLocationProviderClient that will be used to determine the current location.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Monitors the permission
    private var permissionGranted = false


    /**
     * Setup before the log call.
     *
     * This is called *internally* only and not by end users.
     *
     * @return true if setup is successful.
     *
     * @suppress Could have expanded the setup to check for permissions and accuracy conditions
     * @suppress but the the checkSelfPermission call requires an applicationContext on Android which
     * @suppress is delegated to the log call as it is needed there also.
     *
     */
    internal actual fun setup(): Boolean {

        /**
         * Setup will simply be to initialize to the highest priority: PRIORITY_HIGH_ACCURACY and reset the permission.
         */
        return try {
            priority = PRIORITY_HIGH_ACCURACY
            permissionGranted = false
            true
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Exception: ${LocationSetupException().message}"
            )
            false
        }
    }

    /**
     * Gets the current location and posts a JSON body to https://httpbin.org
     * with a required ApplicationContext [context], an optional Long [timestamp], and a String [extra] string.
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
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    actual fun log (context: ApplicationContext, timestamp: Long?, extra: String?) {

        if (setup() && isProviderAvailable(context)) {

            when (PackageManager.PERMISSION_GRANTED) {
                // When ACCESS_FINE_LOCATION is provided, great! just initialize the fusedLocationProviderClient
                ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) -> {
                    permissionGranted = true
                    if (!this::fusedLocationProviderClient.isInitialized)
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
                }
                // When ACCESS_COARSE_LOCATION is provided, fallback to PRIORITY_BALANCED_POWER_ACCURACY
                ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) -> {
                    permissionGranted = true
                    priority = PRIORITY_BALANCED_POWER_ACCURACY
                    if (!this::fusedLocationProviderClient.isInitialized)
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
                    Log.w(TAG, "Warning: ${LocationPrecisionException().message}")
                }
                // No location permission is provided, set [permissionGranted] to false so to proceed with (0, 0) coordinates as required and log an error message.
                else -> {
                    Log.e(TAG, "Exception: ${LocationPermissionException().message}")
                    permissionGranted = false
                }
            }

            // If a location permission was provided, proceed to request a fresh location update.
            if (permissionGranted) {

                // For Android, when running Q+, ACCESS_BACKGROUND_LOCATION is required so location can be accessed while app is in background,
                // print a warning message if it is not granted.
                if (runningQorLater && PackageManager.PERMISSION_GRANTED != context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    Log.w(TAG, "Warning: ${LocationBackgroundPermissionException().message}")
                }

                // Fetches a fresh location entry, GUARANTEED when ACCESS_FINE_LOCATION is provided; NOT GUARANTEED when ACCESS_COARSE_LOCATION is provided (fall to (0.0)).
                // Token not needed for now as the request will terminate in few seconds if it fails to get a fresh location although it might be better
                // to keep track of the cancellation token in case we do not want to allow multiple calls; but it all depends on the requirements!
                fusedLocationProviderClient.getCurrentLocation(priority, CancellationTokenSource().token)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentLocation = task.result
                            // This location could still be null when ACCESS_COARSE_LOCATION is provided, in this case proceed with a (0,0) location
                            if (currentLocation != null) {
                                val currentLocationCoordinates =
                                    LocationCoordinates(
                                        currentLocation.latitude,
                                        currentLocation.longitude,
                                    )
                                postToRemoteServer(currentLocationCoordinates, timestamp, extra)
                            }
                            // If the task is null proceed with (0,0); this might happen when working with ACCESS_COARSE_LOCATION as the docs say.
                            else {
                                postToRemoteServerWithZeroes(timestamp, extra)
                            }
                        }
                        // If the task fails for some weird reason proceed with a (0,0) location also
                        else {
                            postToRemoteServerWithZeroes(timestamp, extra)
                        }

                    }
            } else {
                // Proceed with 0,0 coordinates when no permissions as required
                postToRemoteServerWithZeroes(timestamp, extra)
                Log.e(TAG, "Exception: No location provider is available")
            }
        }
    }


    /**
     *
     * Checks if a location provider is available.
     *
     */
    private fun isProviderAvailable(context: ApplicationContext): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
    }

    /**
     *
     * Launches the coroutine for posting the location
     *
     */
    private fun postToRemoteServer(locationCoordinates: LocationCoordinates, timestamp: Long?, extra: String?) {
        Log.i(TAG, "Your location: LAT -> ${locationCoordinates.latitude}, LON -> ${locationCoordinates.longitude}")
        GlobalScope.launch(Dispatchers.IO) {
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
        private val runningQorLater = Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
    }
}