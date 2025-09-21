plugins {
    kotlin("jvm") version "1.9.20" apply false
}

allprojects {
    group = "com.brainiac"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    dependencies {
        val implementation by configurations
        val testImplementation by configurations
        
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}