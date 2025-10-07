package com.duchastel.simon.brainiac.core.process

interface CoreLoopProcess {
    suspend fun processUserPrompt(userPrompt: String): String
}
