plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
            }
        }
        val linuxX64Test by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
            }
        }
        val macosX64Test by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
            }
        }
        val macosArm64Test by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
            }
        }
        val mingwX64Test by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}