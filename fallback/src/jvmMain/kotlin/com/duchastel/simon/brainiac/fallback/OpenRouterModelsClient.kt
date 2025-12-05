package com.duchastel.simon.brainiac.fallback

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Client for fetching model information from OpenRouter's models API.
 */
class OpenRouterModelsClient(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }
) {
    /**
     * Fetches all available models from OpenRouter.
     *
     * @return List of all models with their metadata
     */
    suspend fun fetchAllModels(): List<OpenRouterModel> {
        val response = httpClient.get("https://openrouter.ai/api/v1/models") {
            contentType(ContentType.Application.Json)
        }.body<OpenRouterModelsResponse>()

        return response.data
    }

    /**
     * Fetches only free models (models with zero pricing for prompt and completion).
     *
     * @return List of free models
     */
    suspend fun fetchFreeModels(): List<OpenRouterModel> {
        return fetchAllModels().filter { model ->
            model.pricing.prompt.toDoubleOrNull() == 0.0 &&
            model.pricing.completion.toDoubleOrNull() == 0.0
        }
    }

    fun close() {
        httpClient.close()
    }
}

/**
 * Response from the OpenRouter /api/v1/models endpoint.
 */
@Serializable
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModel>
)

/**
 * Represents a model available on OpenRouter.
 */
@Serializable
data class OpenRouterModel(
    val id: String,
    val name: String,
    val created: Long? = null,
    val description: String? = null,
    @SerialName("context_length")
    val contextLength: Int? = null,
    val pricing: ModelPricing,
    @SerialName("top_provider")
    val topProvider: TopProvider? = null,
    val architecture: Architecture? = null,
    @SerialName("per_request_limits")
    val perRequestLimits: PerRequestLimits? = null,
)

/**
 * Pricing information for a model.
 */
@Serializable
data class ModelPricing(
    val prompt: String,
    val completion: String,
    val image: String? = null,
    val request: String? = null,
)

/**
 * Top provider information.
 */
@Serializable
data class TopProvider(
    @SerialName("context_length")
    val contextLength: Int? = null,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    @SerialName("is_moderated")
    val isModerated: Boolean? = null,
)

/**
 * Model architecture information.
 */
@Serializable
data class Architecture(
    val modality: String? = null,
    val tokenizer: String? = null,
    @SerialName("instruct_type")
    val instructType: String? = null,
)

/**
 * Rate limits per request.
 */
@Serializable
data class PerRequestLimits(
    @SerialName("prompt_tokens")
    val promptTokens: String? = null,
    @SerialName("completion_tokens")
    val completionTokens: String? = null,
)
