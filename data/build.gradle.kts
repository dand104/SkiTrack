plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
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
    implementation("androidx.core:core-ktx:1.15.0")

    // Coroutines support for Flow and async tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Google Mobile Services Location
    "gmsImplementation"("com.google.android.gms:play-services-location:21.3.0")

}