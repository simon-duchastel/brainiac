package com.duchastel.simon.brainiac.agents.gemini

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class Gemini25ProTest : StringSpec({

    "should successfully parse API response and return text" {
        val mockEngine = MockEngine { request ->
            // Verify the request
            request.url.toString() shouldContain "gemini-2.5-pro:generateContent"
            request.headers["x-goog-api-key"] shouldBe "test-api-key"

            // Return successful response
            respond(
                content = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    {
                                        "text": "Hello from Gemini!"
                                    }
                                ]
                            },
                            "finishReason": "STOP",
                            "index": 0
                        }
                    ]
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("test-api-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe "Hello from Gemini!"
    }

    "should return null when API returns error status code" {
        val mockEngine = MockEngine {
            respond(
                content = """{"error": {"message": "Invalid API key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("invalid-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe null
    }

    "should return null when response has no candidates" {
        val mockEngine = MockEngine {
            respond(
                content = """{"candidates": []}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("test-api-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe null
    }

    "should return null when candidate has no parts" {
        val mockEngine = MockEngine {
            respond(
                content = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": []
                            }
                        }
                    ]
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("test-api-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe null
    }

    "should handle network errors gracefully" {
        val mockEngine = MockEngine {
            throw Exception("Network connection failed")
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("test-api-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe null
    }

    "should handle response with multiple candidates by taking first" {
        val mockEngine = MockEngine {
            respond(
                content = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    {
                                        "text": "First response"
                                    }
                                ]
                            },
                            "index": 0
                        },
                        {
                            "content": {
                                "parts": [
                                    {
                                        "text": "Second response"
                                    }
                                ]
                            },
                            "index": 1
                        }
                    ]
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("test-api-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe "First response"
    }

    "should ignore unknown fields in response" {
        val mockEngine = MockEngine {
            respond(
                content = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    {
                                        "text": "Response text"
                                    }
                                ]
                            },
                            "unknownField": "should be ignored",
                            "safetyRatings": []
                        }
                    ],
                    "modelVersion": "gemini-2.5-pro"
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testClient = createTestClient(mockEngine)
        val provider = Gemini25Pro("test-api-key", client = testClient)
        val result = provider.process("test input")

        result shouldBe "Response text"
    }
})

/**
 * Creates a test HTTP client with the given mock engine
 */
private fun createTestClient(mockEngine: MockEngine): HttpClient {
    return HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
            })
        }
    }
}
