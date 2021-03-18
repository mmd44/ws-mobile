package me.mmd44.wsmobile


/**
 * A KMM module built on top of Google fused location provider for Android and CLLocationManager for iOS.
 *
 * Sends an HTTP post request to a pre-defined server containing the user's current location info upon calling the log() function.
 *
 * Uses an asynchronous mechanism and no errors should be thrown upon using this module; all errors are handled internally and logged.
 *
 * Ask for permissions should be handled explicitly by developers
 *
 * @suppress For a more practical solution a callback might be added so to handle the final output; but I considered this
 * @suppress to be a silent location provider as I understood from the requirements!
 *
 */
expect class GeoLogger constructor () {

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
    internal fun setup () : Boolean

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
     * The output must be monitored in logs for any failure (Exceptions) or success (HTTP response) with tag [TAG].
     *
     * @suppress The logic is implemented with Tasks and Callbacks for getting the location and COROUTINES are used only when making the POST request;
     * @suppress although a more elegant solution would have been by implementing all the logic using coroutines, I made this decision to make it as consistent as
     * @suppress possible with the native (iOS) implementation on which coroutines are still in an experimental phase.
     *
     *
     */
    fun log (context: ApplicationContext, timestamp:Long?, extra:String?)
}