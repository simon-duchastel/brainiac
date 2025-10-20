package com.duchastel.simon.brainiac.core.process.util

import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.prompt.llm.LLModel

suspend fun <T> AIAgentLLMWriteSession.withModel(withModel: LLModel, block: suspend AIAgentLLMWriteSession.() -> T): T {
    val originalModel = model
    model = withModel
    val result = block()
    model = originalModel
    return result
}