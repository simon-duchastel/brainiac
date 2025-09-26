plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "com.brainiac"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    repositories {
        mavenCentral()
    }
}