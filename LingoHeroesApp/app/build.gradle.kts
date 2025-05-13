plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "com.example.lingoheroesapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.lingoheroesapp"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase dependencies
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Lottie Animation
    implementation("com.airbnb.android:lottie:6.3.0")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Glide dla wczytywania obrazów
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}