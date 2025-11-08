plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "com.duchastel.simon.brainiac.agent"

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.koog.agents)
                api(project(":core-loop"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
