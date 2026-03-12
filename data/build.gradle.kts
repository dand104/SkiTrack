plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp)
}

apply(from = "$rootDir/android-common.gradle")

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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    "gmsImplementation"(libs.google.play.services.location)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}