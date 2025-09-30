plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mokkery)
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
                implementation(project(":core:model"))
                implementation(project(":core:fs"))
                implementation(project(":core:llm"))
                implementation(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotlinx.datetime)
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotest.assertions.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.mockk)
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