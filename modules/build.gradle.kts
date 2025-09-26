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

// Add test task that delegates to multiplatform test tasks
tasks.register("test") {
    group = "verification"
    description = "Runs all tests across all platforms"
    
    // Find all subprojects that have KMP configuration
    val testableProjects = subprojects
        .filter { project -> 
            // Only include projects that have build files
            project.buildFile.exists() 
        }
        .map { project -> 
            "${project.path}:jvmTest" 
        }
    
    dependsOn(testableProjects)
}