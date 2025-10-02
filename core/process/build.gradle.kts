plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()

    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Expose all process submodules to consumers of this module
                api(project(":core:process:core-loop"))
            }
        }
    }
}