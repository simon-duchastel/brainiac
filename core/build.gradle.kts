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
                // Expose all core submodules to consumers of this module
                api(project(":core:identity"))
                api(project(":core:fileaccess"))
                api(project(":core:search"))
                api(project(":core:process"))
            }
        }
    }
}
