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
    val jvmTestTasks = subprojects
        .filter { project -> 
            // Only include projects that have build files
            project.buildFile.exists() 
        }
        .map { project -> 
            "${project.path}:jvmTest" 
        }
    
    val nativeTestTasks = subprojects
        .filter { project -> 
            // Only include projects that have build files
            project.buildFile.exists() 
        }
        .flatMap { project ->
            listOf(
                "${project.path}:linuxX64Test",
                "${project.path}:macosX64Test", 
                "${project.path}:macosArm64Test",
                "${project.path}:mingwX64Test"
            )
        }
    
    dependsOn(jvmTestTasks + nativeTestTasks)
}

// Add native build tasks
tasks.register("buildNative") {
    group = "build"
    description = "Builds native binaries for all platforms"
    
    val nativeBuildTasks = subprojects
        .filter { project -> 
            project.buildFile.exists() 
        }
        .flatMap { project ->
            listOf(
                "${project.path}:linuxX64Binaries",
                "${project.path}:macosX64Binaries", 
                "${project.path}:macosArm64Binaries",
                "${project.path}:mingwX64Binaries"
            )
        }
    
    dependsOn(nativeBuildTasks)
}