import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val kotlinVersion = "1.4.31"
val ktorVersion = "1.5.2"
val jsonSerializerVersion = "1.0.1"
val coroutinesCoreVersion = "1.4.2-native-mt"
val googleMaterialVersion = "1.3.0"
val googlePlayServicesVersion = "18.0.0"
val coroutinesAndroidVersion = "1.3.7"
val dateTimeVersion = "0.1.1"
val androidXTestCoreVersion = "1.4.0-alpha04"
val robolectricVersion = "4.3.1"
val androidXTestExtKotlinRunnerVersion = "1.1.2"
val mockitoVersion = "2.23.4"
val mockitoDexMakerVersion = "2.19.1"
val espressoVersion = "3.2.0"

plugins {
    kotlin("multiplatform") version "1.4.31"
    id("com.android.library")
    id("maven-publish")
    kotlin("plugin.serialization") version "1.4.31"
}

group = "me.mmd44"
version = "1.0.0"

repositories {
    google()
    jcenter()
    mavenCentral()
}

kotlin {

    android {
        publishLibraryVariants("debug", "release")
    }

    ios {
        binaries {
            framework {
                baseName = "wsmobile"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation ("io.ktor:ktor-client-mock:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$jsonSerializerVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$dateTimeVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))

                implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesAndroidVersion")

                implementation ("org.mockito:mockito-core:$mockitoVersion")
                implementation ("org.mockito:mockito-inline:3.4.0")
                implementation ("com.linkedin.dexmaker:dexmaker-mockito:$mockitoDexMakerVersion")

                implementation ("androidx.test.espresso:espresso-core:$espressoVersion")
                //implementation ("androidx.test.espresso:espresso-contrib:$espressoVersion")
                //implementation ("androidx.test.espresso:espresso-intents:$espressoVersion")
                implementation ("androidx.test.espresso:espresso-idling-resource:$espressoVersion")
                implementation ("androidx.test.espresso.idling:idling-concurrent:$espressoVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.google.android.material:material:$googleMaterialVersion")
                implementation("com.google.android.gms:play-services-location:$googlePlayServicesVersion")
                implementation("io.ktor:ktor-client-android:$ktorVersion")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
                implementation ("androidx.test.ext:junit-ktx:$androidXTestExtKotlinRunnerVersion")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:$ktorVersion")
            }
        }
        val iosTest by getting
    }
}


android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(18)
        targetSdkVersion(30)
    }

    testOptions.unitTests {
        it.isIncludeAndroidResources = true
        it.isReturnDefaultValues = true
    }
}
buildscript {
    val kotlinVersion by extra("1.4.31")
    dependencies {
        "classpath"("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework =
        kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(buildDir, "xcode-frameworks")
    from({ framework.outputDirectory })
    into(targetDir)
}
tasks.getByName("build").dependsOn(packForXcode)

val packForIphone by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = "iphoneos"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework =
        kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(buildDir, "iphone-frameworks")
    from({ framework.outputDirectory })
    into(targetDir)
}
tasks.getByName("build").dependsOn(packForIphone)

