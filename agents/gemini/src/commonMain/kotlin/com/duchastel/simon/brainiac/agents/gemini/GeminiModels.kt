package com.duchastel.simon.brainiac.agents.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for Gemini API
 */
@Serializable
internal data class GeminiRequest(
    val contents: List<Content>
)

/**
 * Content object containing message parts and optional role
 */
@Serializable
internal data class Content(
    val parts: List<Part>,
    val role: String? = null
)

/**
 * A part of a message, currently supporting text only
 */
@Serializable
internal data class Part(
    val text: String
)

/**
 * Response payload from Gemini API
 */
@Serializable
internal data class GeminiResponse(
    val candidates: List<Candidate>
)

/**
 * A candidate response from the model
 */
@Serializable
internal data class Candidate(
    val content: Content,
    @SerialName("finishReason")
    val finishReason: String? = null,
    val index: Int? = null
)
