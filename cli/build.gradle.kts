plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
}

group = "com.duchastel.simon.brainiac.cli"

kotlin {
    jvmToolchain(17)
    jvm()
    linuxX64("linux") {
        binaries {
            executable {
                entryPoint = "com.duchastel.simon.brainiac.cli.main"
            }
        }
    }
    macosX64("macos") {
        binaries {
            executable {
                entryPoint = "com.duchastel.simon.brainiac.cli.main"
            }
        }
    }
    macosArm64("macosArm") {
        binaries {
            executable {
                entryPoint = "com.duchastel.simon.brainiac.cli.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core-agent"))
                implementation(project(":tools"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.okio)
                implementation(libs.circuit.foundation)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.slf4j.nop)
                implementation(libs.mosaic.runtime)
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val linuxMain by getting {
            dependsOn(nativeMain)
        }
        val macosMain by getting {
            dependsOn(nativeMain)
        }
        val macosArmMain by getting {
            dependsOn(nativeMain)
        }
    }
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}