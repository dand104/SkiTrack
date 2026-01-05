plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(from = "$rootDir/gradle/android-common.gradle")

android {
    namespace = "org.skitrace.skitrace.data"
}

dependencies {
    implementation(project(":core"))

    // Coroutines support for Flow and async tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}