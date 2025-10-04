plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "com.brainiac.core.process.search"

kotlin {

    jvm()

    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:fileaccess:api"))
            }
        }
    }
}
