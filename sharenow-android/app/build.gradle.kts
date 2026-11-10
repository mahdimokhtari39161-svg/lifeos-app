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
        versionCode = 8
        versionName = "2.5.0"
    }

    signingConfigs {
        create("shareNowDebug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shareNowDebug")
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


configurations.configureEach {
    // The AndroidX Activity Result API and the ZXing scanner bring Kotlin
    // standard-library variants transitively. Keep the merged Android classpath
    // free of the legacy split JDK7/JDK8 jars that duplicate classes.
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
