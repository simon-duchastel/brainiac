plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "com.duchastel.simon.brainiac.agent"

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                // Koog agents for AIAgent, ToolRegistry, etc.
                api(libs.koog.agents)

                // Coroutines for async operations
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
