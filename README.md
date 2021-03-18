# WS-Mobile

A KMM module built on top of Google fused location provider for Android and CLLocationManager for iOS.

* Sends an HTTP post request to a pre-defined server containing the user's current location info (latitude and longitude) upon calling the log() function.


* The log call takes as parameters: a long timestamp and a string extra to be posted with the JSON; if the timestamp is provided as null/nil it will be auto-generated, the extra will remain as provided.


* Uses an asynchronous mechanism (Tasks and Delegates) and no errors should be thrown upon using this module; all errors are handled internally and logged.


* Asking for location permissions should be handled explicitly by developers; if not provided, log calls will not work.

## Basic Usage (local)

### iOS (Physical and Simulator)

#### Install

Built the KMM for a physical device or simulator iOS device from the root of this module using:
```kotlin
./gradlew packForIphone
```
or
```kotlin
./gradlew packForXcode
```
Add it as a framework dependency to the *.xcodeproj* file in x-code *General* settings and add the module directory to the *Build Settings -> Framework Search Paths*.

#### Usage

Create an instance of GeoLogger with empty constructor.

Call the log function with 3 parameters: Any, long timestamp or nil, and extra string or mull.


```swift
let geolog = GeoLogger()
geolog.log(context: (), timestamp: nil, extra: nil)
```

#### Known issues
A line is shown in logs after the first HTTP post request; it seems something related to using coroutines on iOS (nw_protocol_get_quic_image_block_invoke dlopen libquic failed).

### Android

#### Install

Built the KMM for android from the root of this module using:
```kotlin
./gradlew publishToMavenLocal
```
Make sure the module is generated under *USER_HOME/.m2/repository*

Add *mavenLocal()* to *buildscript -> repositories* and *allprojects* in the project level gradle file (make sure it is in the first line).
```kotlin
buildscript {
    ...
    repositories {
        mavenLocal()
        google()
        jcenter()
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }
}
```

Add it to gradle dependency

Debug Variant:
```kotlin
implementation 'me.mmd44:ws-mobile-kotlin-android-debug:1.0.0'
```

#### Usage

Create an instance of GeoLogger.

Call the log function with 3 parameters: a required applicationContext, long timestamp or null, and an extra string or null.


```kotlin
val geoLogger = GeoLogger()
geoLogger.log(requireActivity().application, null, null)
```

#### Known issues
When coarse permission is provided on Android, Google's fused location provider fails to determine the location sometimes.


## Examples

This folder contains sample apps for Android and iOS.

