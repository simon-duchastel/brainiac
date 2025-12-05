plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                // Koog agents framework
                api(libs.koog.agents)

                // HTTP client for OpenRouter API
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                // Serialization
                implementation(libs.kotlinx.serialization.core)

                // Async
                implementation(libs.kotlinx.coroutines.core)

                // Logging
                implementation(libs.slf4j.simple)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}
