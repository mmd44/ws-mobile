package me.mmd44.wsmobile

/**
 *
 *  Exceptions but used as messages only for now as no exception is thrown from this module.
 *
 */

internal class LocationNotEnabledException(message: String = "Location is not enabled"): Exception(message)

internal class LocationPrecisionException(message: String = "Provide fine/full accuracy permission for better results"): Exception(message)

internal class LocationSetupException(message: String = "An error has occurred on setup"): Exception(message)

internal class LocationPermissionException(message: String = "Location permissions are not granted"): Exception(message)

internal class LocationBackgroundPermissionException(message: String = "Logs will only work when app is on foreground; " +
        "Provide ACCESS_BACKGROUND_LOCATION permission for Q+ devices to send logs when app is in background"): Exception(message)