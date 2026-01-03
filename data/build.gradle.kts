plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(from = "$rootDir/gradle/android-common.gradle.kts")

android {
    namespace = "org.skitrace.skitrace.data"
}

dependencies {
    implementation(project(":core"))
}