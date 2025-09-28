package com.brainiac.core.llm

interface LLMService {
    fun generateSearchQueries(userPrompt: String, context: String): List<String>
    
    fun generateResponse(workingMemory: String): String
}