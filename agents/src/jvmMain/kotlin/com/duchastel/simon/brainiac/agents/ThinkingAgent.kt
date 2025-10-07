package com.duchastel.simon.brainiac.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.clients.google.GoogleModels
import com.duchastel.simon.brainiac.core.agent.Agent

/**
 * ThinkingAgent - An AI agent implementation using Google's Gemini via the koog framework.
 *
 * This agent provides thoughtful, reasoned responses using Google's Gemini model
 * through the JetBrains koog framework for reliable AI agent capabilities.
 *
 * @param apiKey The Google AI API key for authentication
 */
class ThinkingAgent(
    apiKey: String
) : Agent {

    private val koogAgent: AIAgent<String, String> = AIAgent(
        promptExecutor = simpleGoogleAIExecutor(apiKey),
        llmModel = GoogleModels.Gemini2_0Flash,
        systemPrompt = "You are a thoughtful AI assistant focused on helping users think through problems and providing clear, reasoned responses.",
        temperature = 0.7
    )

    override suspend fun process(input: String): String? {
        return try {
            val result = koogAgent.run(input)
            result
        } catch (e: Exception) {
            println("ThinkingAgent error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
