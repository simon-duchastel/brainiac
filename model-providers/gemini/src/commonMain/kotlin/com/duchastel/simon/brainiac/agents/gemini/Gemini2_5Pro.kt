package com.duchastel.simon.brainiac.agents.gemini

import com.duchastel.simon.brainiac.core.process.ModelProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Implementation of ModelProvider for Google's Gemini 2.5 Pro model.
 *
 * @param apiKey The Google AI API key for authentication
 * @param baseUrl The base URL for the Gemini API (defaults to the official endpoint)
 */
class Gemini2_5Pro(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com"
) : ModelProvider {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
            })
        }
    }

    private val modelEndpoint = "$baseUrl/v1beta/models/gemini-2.5-pro:generateContent"

    override fun process(input: String): String? = runBlocking {
        try {
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
}

@Serializable
internal data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
internal data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
internal data class Part(
    val text: String
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<Candidate>
)

@Serializable
internal data class Candidate(
    val content: Content,
    @SerialName("finishReason")
    val finishReason: String? = null,
    val index: Int? = null
)
