plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()

    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Expose API modules to consumers - they only see interfaces
                api(project(":core:process:core-loop:api"))
                api(project(":core:process:search:api"))
            }
        }
    }
}