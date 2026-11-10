plugins {
    id("com.android.application")
}

android {
    namespace = "com.sharenow.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sharenow.transfer"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.1.1"
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/NOTICE")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
