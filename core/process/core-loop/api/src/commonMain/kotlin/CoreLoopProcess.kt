package com.brainiac.core.process

interface CoreLoopProcess {
    fun processUserPrompt(userPrompt: String): String
}
