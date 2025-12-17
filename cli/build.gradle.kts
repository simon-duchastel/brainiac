plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
}

group = "com.duchastel.simon.brainiac.cli"

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    linuxX64("native") {
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
        val nativeMain by getting
    }
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}