plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.duchastel.simon.brainiac.tools"

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                // Koog agents for Tool interface
                api(libs.koog.agents)

                // HTTP client for Tavily API
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                // Kotlinx serialization for JSON
                implementation(libs.kotlinx.serialization.core)

                // Coroutines for async operations
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.framework.engine)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}
