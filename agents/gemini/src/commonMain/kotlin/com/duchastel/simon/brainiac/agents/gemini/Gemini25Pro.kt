package com.duchastel.simon.brainiac.agents.gemini

import com.duchastel.simon.brainiac.core.agent.Agent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Implementation of Agent for Google's Gemini 2.5 Pro model.
 *
 * @param apiKey The Google AI API key for authentication
 * @param client The HTTP client to use (defaults to a configured client, injectable for testing)
 */
class Gemini25Pro(
    private val apiKey: String,
    private val client: HttpClient = createDefaultClient()
) : Agent {

    private val modelEndpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent"

    override suspend fun process(input: String): String? {
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = input))
                    )
                )
            )

            val response: HttpResponse = client.post(modelEndpoint) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val geminiResponse: GeminiResponse = response.body()
                geminiResponse.candidates.firstOrNull()
                    ?.content?.parts?.firstOrNull()
                    ?.text
            } else {
                println("Gemini API error: ${response.status} - ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            println("Error calling Gemini API: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    companion object {
        /**
         * Creates the default HttpClient with JSON content negotiation configured
         * for Gemini API responses.
         */
        fun createDefaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                    isLenient = true
                })
            }
        }
    }
}
