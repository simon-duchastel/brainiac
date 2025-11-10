import com.adarshr.gradle.testlogger.TestLoggerExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.test.logger)
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}

allprojects {
    group = "com.duchastel.simon.brainiac"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    repositories {
        mavenCentral()
        google()
    }

    apply {
        plugin("com.adarshr.test-logger")
    }

    configure<TestLoggerExtension> {
        showPassed = true
        showSkipped = true
        showFailed = true
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs all JVM tests"

    // Find all subprojects that have build files
    val jvmTestTasks = subprojects
        .filter { project ->
            project.buildFile.exists()
        }
        .map { project ->
            "${project.path}:jvmTest"
        }

    dependsOn(jvmTestTasks)
}