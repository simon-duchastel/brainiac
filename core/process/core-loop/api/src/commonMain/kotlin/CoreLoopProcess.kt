package com.duchastel.simon.brainiac.core.process

interface CoreLoopProcess {
    fun processUserPrompt(userPrompt: String): String
}
