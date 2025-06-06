import org.apache.tools.ant.util.JavaEnvUtils.VERSION_11
import org.apache.tools.ant.util.JavaEnvUtils.VERSION_1_8

val implementation: Unit
    get() {
        TODO()
    }



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

    buildTypes {
        release {
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
            exclude("META-INF/AL2.0")
            exclude("META-INF/LGPL2.1")
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

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}