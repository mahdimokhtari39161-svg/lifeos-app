plugins {
    id("com.android.application")
}

android {
    namespace = "com.sharenow.android"
    compileSdk = 35

    defaultConfig {
        // New package ID prevents conflicts with older ShareNow builds signed with
        // a different debug certificate. Future builds keep this same ID and key.
        applicationId = "com.sharenow.transfer2026"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "2.4.0"
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
    implementation("androidx.activity:activity:1.9.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
