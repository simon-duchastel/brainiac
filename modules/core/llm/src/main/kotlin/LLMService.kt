package com.brainiac.core.llm

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.SearchToolProvider

interface LLMService {
    fun generateSearchQueries(userPrompt: String, context: String): List<String>
    
    fun generateResponse(workingMemory: String): String
    
    fun searchWithTools(query: String, toolProvider: SearchToolProvider): List<LTMFile>
}