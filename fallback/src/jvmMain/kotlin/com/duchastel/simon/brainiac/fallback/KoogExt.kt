package com.duchastel.simon.brainiac.fallback

import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.prompt.llm.LLModel

/**
 * Temporarily switches the LLM model for a block of code.
 *
 * This utility allows you to use a different model for specific operations
 * and then automatically restores the original model.
 */
suspend fun <T> AIAgentLLMWriteSession.withModel(
    withModel: LLModel,
    block: suspend AIAgentLLMWriteSession.() -> T
): T {
    val originalModel = model
    model = withModel
    val result = block()
    model = originalModel
    return result
}
