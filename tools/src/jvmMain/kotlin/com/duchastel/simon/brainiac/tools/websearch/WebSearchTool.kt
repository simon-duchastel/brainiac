package com.duchastel.simon.brainiac.tools.websearch

import ai.koog.agents.core.tools.SimpleTool
import com.duchastel.simon.brainiac.tools.BrainiacTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

/**
 * Arguments for web search tool.
 *
 * @property query The search query to execute
 */
@Serializable
data class WebSearchArgs(
    val query: String
)

/**
 * Web search tool powered by Tavily API.
 *
 * This tool allows AI agents to search the web for information.
 * @property apiKey The Tavily API key
 * @property maxResults Maximum number of search results to return per query
 *
 * @see TavilyClient
 */
class WebSearchTool(
    private val apiKey: String,
    private val maxResults: Int = 5,
) : BrainiacTool {
    private val tavilyClient = TavilyClient(apiKey)

    override fun toKoogTool(): SimpleTool<WebSearchArgs> {
        return object : SimpleTool<WebSearchArgs>() {
            override val name: String = "web_search"

            override val description: String = """
                Search the web for current information using a specialized search API.
                This tool is useful when you need up-to-date information, facts, or data
                that may not be in your training data. Returns relevant web search results
                with titles, URLs, and content snippets in JSON format.
            """.trimIndent()

            override val argsSerializer: KSerializer<WebSearchArgs> = serializer()

            override suspend fun doExecute(args: WebSearchArgs): String {
                return try {
                    val response = tavilyClient.search(
                        query = args.query,
                        maxResults = maxResults,
                        includeAnswer = true,
                    )

                    // Format response as JSON string
                    buildJsonObject {
                        put("query", response.query)
                        response.answer?.let { put("answer", it) }
                        putJsonArray("results") {
                            response.results.forEach { result ->
                                addJsonObject {
                                    put("title", result.title)
                                    put("url", result.url)
                                    put("content", result.content)
                                    put("score", result.score)
                                }
                            }
                        }
                    }.toString()
                } catch (e: Exception) {
                    """{"error": "${e.message}"}"""
                }
            }
        }
    }

    fun close() {
        tavilyClient.close()
    }
}
