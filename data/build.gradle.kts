plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.3.4"
}

apply(from = "$rootDir/gradle/android-common.gradle")

android {
    namespace = "org.skitrace.skitrace.data"

    flavorDimensions += "services"

    productFlavors {
        register("aosp") {
            dimension = "services"
        }
        register("gms") {
            dimension = "services"
        }
    }
}

dependencies {
    implementation(project(":core"))

    // Core Kotlin
    implementation("androidx.core:core-ktx:1.17.0")

    // Coroutines support for Flow and async tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Google Mobile Services Location
    "gmsImplementation"("com.google.android.gms:play-services-location:21.3.0")

    // Room Database
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}