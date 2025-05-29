// Project-level build.gradle
/*plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.google.gms:google-services:4.4.2") // Firebase services
    }
}*/



buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.android.tools.build:gradle:8.4.2") // Wersja AGP
    }
}

// Top-level plugins configuration
plugins {
    id("com.android.application") version "8.2.2" apply false // Definicja wtyczki AGP
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false // Wersja Kotlin
    id("com.google.gms.google-services") version "4.4.2" apply false // Wtyczka Google Services
}
