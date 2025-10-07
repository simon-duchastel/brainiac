plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

group = "com.duchastel.simon.brainiac.agents"

kotlin {
    jvm()

    // Note: koog currently only supports JVM, JS, WASM, and iOS targets
    // Native desktop targets (linuxX64, macosX64, etc.) are not supported yet

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":core:agents:api"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.koog.agents)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.framework.engine)
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
