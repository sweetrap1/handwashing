plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jarindimick.handwashtracking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarindimick.handwashtracking"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Define a flavor dimension
    flavorDimensions += "version" // You can name this anything relevant, e.g., "pricing"

    // Define product flavors for free and pro versions
    productFlavors {
        create("free") {
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
        }
        create("pro") {
            // Pro version will use the default applicationId and versionName
            // No suffix needed for the pro version as it's the main app
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { // Add a debug build type if not present
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        resources {
            // Corrected way to exclude resources, addressing deprecation warnings
            excludes.add("META-INF/AL2.0")
            excludes.add("META-INF/LGPL2.1")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation ("org.mindrot:jbcrypt:0.4")
    implementation ("androidx.recyclerview:recyclerview:1.4.0")

    // CameraX libraries (core includes the analysis use case)
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    // Guava dependency for ListenableFuture, specifically the Android version
    implementation("com.google.guava:guava:32.1.3-android")

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")

    // Added for @Nullable annotation
    implementation("androidx.annotation:annotation:1.7.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Google Mobile Ads SDK:
    // We add it to 'freeImplementation' so it's only linked into the free app runtime.
    // However, for AAPT2 to recognize the 'ads:' attributes in XML for *all* flavors,
    // a small trick is sometimes needed or you can add it to all debug/release builds.
    // A robust way is to declare it as 'implementation' if its attributes are used across all layouts,
    // and let the code handle visibility.
    // Let's move it to `implementation` to resolve the resource linking issue for Pro variant.
    // The Java code already hides ads for Pro.
    implementation("com.google.android.gms:play-services-ads:24.4.0")


    // Google Play Billing Library for in-app purchases (for both free and pro to handle upgrades)
    implementation("com.android.billingclient:billing:7.1.1")
}
