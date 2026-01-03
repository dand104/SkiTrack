import com.android.build.gradle.BaseExtension

val android = extensions.findByName("android") as? BaseExtension
    ?: error("Android plugin must be applied before applying this script")

android.apply {
    compileSdk = 36

    defaultConfig {
        minSdk = 32
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}