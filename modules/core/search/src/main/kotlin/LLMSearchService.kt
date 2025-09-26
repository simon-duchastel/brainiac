package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.SearchToolProvider
import com.brainiac.core.llm.LLMService

class LLMSearchService(
    private val llmService: LLMService,
    private val searchToolProvider: SearchToolProvider
) : SearchService {

    override fun searchLTM(queries: List<String>): List<LTMFile> {
        if (queries.isEmpty()) {
            return emptyList()
        }

        // Combine all queries into a single search request for the LLM
        val combinedQuery = buildString {
            appendLine("Search the long-term memory for information relevant to the following queries:")
            queries.forEachIndexed { index, query ->
                appendLine("${index + 1}. $query")
            }
            appendLine()
            appendLine("Please use the available tools to explore the memory files and return the most relevant LTM files.")
            appendLine("Focus on semantic relevance, contextual relationships, and practical usefulness for answering the queries.")
        }

        return llmService.searchWithTools(combinedQuery, searchToolProvider)
    }
}