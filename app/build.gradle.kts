plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.threadopsy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.threadopsy"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Forces transitive dependencies to stay on API 34 / AGP 8.2.2 compatible versions
configurations.all {
    resolutionStrategy {
        force(
            "androidx.core:core:1.12.0",
            "androidx.core:core-ktx:1.12.0",
            "androidx.activity:activity:1.8.2",
            "androidx.activity:activity-ktx:1.8.2",
            "androidx.activity:activity-compose:1.8.2",
            "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0",
            "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0",
            "androidx.lifecycle:lifecycle-runtime-compose:2.7.0"
        )
    }
}

dependencies {
    // --- Core Android & Jetpack Compose (Locked for compileSdk 34 & AGP 8.2.2) ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("com.google.firebase:firebase-auth")
    // Compose BOM 2024.02.02 maps to Compose 1.6.x (Built for API 34)
    val composeBom = platform("androidx.compose:compose-bom:2024.02.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- 1. ViewModel & Extended Icons for our RPG Forensic UI ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended")

    // --- 2. CameraX (API 34 compatible version) ---
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // --- 3. Google ML Kit Text Recognition ---
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // --- 4. Firebase Firestore (Online Closet Sync) ---
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-firestore")

    // --- Testing Dependencies (Also locked to Compose BOM 2024.02.02) ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}