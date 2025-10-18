plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "com.duchastel.simon.brainiac.cli"

kotlin {
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("com.duchastel.simon.brainiac.cli.MainKt")
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":core-loop"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
