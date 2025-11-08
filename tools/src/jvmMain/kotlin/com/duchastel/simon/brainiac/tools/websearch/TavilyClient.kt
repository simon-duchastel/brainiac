package com.duchastel.simon.brainiac.tools.websearch

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
 * HTTP client for the Tavily Search API.
 *
 * Tavily is a search API specifically designed for AI agents and LLMs,
 * focusing on high-quality, citable sources that reduce hallucinations.
 *
 * @property apiKey The Tavily API key (get one from https://app.tavily.com)
 * @see <a href="https://docs.tavily.com">Tavily Documentation</a>
 */
class TavilyClient(
    private val apiKey: String,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }
) {
    /**
     * Performs a web search using the Tavily API.
     *
     * @param query The search query
     * @param maxResults Maximum number of results to return (default: 5)
     * @param includeAnswer Whether to include an AI-generated answer summary
     * @return Search results from Tavily
     */
    suspend fun search(
        query: String,
        maxResults: Int = 5,
        includeAnswer: Boolean = false,
    ): TavilySearchResponse {
        val request = TavilySearchRequest(
            query = query,
            apiKey = apiKey,
            maxResults = maxResults,
            includeAnswer = includeAnswer,
        )

        return httpClient.post("https://api.tavily.com/search") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    fun close() {
        httpClient.close()
    }
}

@Serializable
private data class TavilySearchRequest(
    val query: String,
    @SerialName("api_key")
    val apiKey: String,
    @SerialName("max_results")
    val maxResults: Int = 5,
    @SerialName("include_answer")
    val includeAnswer: Boolean = false,
)

@Serializable
data class TavilySearchResponse(
    val query: String,
    val answer: String? = null,
    val results: List<TavilySearchResult>,
    @SerialName("response_time")
    val responseTime: Double? = null,
)

@Serializable
data class TavilySearchResult(
    val title: String,
    val url: String,
    val content: String,
    val score: Double,
    @SerialName("raw_content")
    val rawContent: String? = null,
    @SerialName("published_date")
    val publishedDate: String? = null,
)
