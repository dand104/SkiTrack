plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(from = "$rootDir/gradle/android-common.gradle.kts")

android {
    namespace = "org.skitrace.skitrace.core"

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags("")
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cxx/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
}