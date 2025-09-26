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
    
    // Only depend on projects that have KMP configuration (have build.gradle.kts)
    val testableProjects = listOf(
        ":app:jvmTest",
        ":core:fs:jvmTest", 
        ":core:identity:jvmTest",
        ":core:llm:jvmTest",
        ":core:model:jvmTest",
        ":core:process:jvmTest",
        ":core:search:jvmTest",
        ":llm-adapter:jvmTest"
    )
    dependsOn(testableProjects)
}