plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

group = "com.brainiac.agents"

kotlin {
    jvm()

    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":agents:api"))
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val linuxX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val macosX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val macosArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val mingwX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
}
