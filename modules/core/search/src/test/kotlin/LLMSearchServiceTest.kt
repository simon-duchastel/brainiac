package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.LTMFrontmatter
import com.brainiac.core.model.SearchToolProvider
import com.brainiac.core.llm.LLMService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.slot
import java.time.Instant

class LLMSearchServiceTest : StringSpec({

    "should return empty list when queries are empty" {
        val mockLLMService = mockk<LLMService>()
        val mockToolProvider = mockk<SearchToolProvider>()
        val searchService = LLMSearchService(mockLLMService, mockToolProvider)
        
        val result = searchService.searchLTM(emptyList())
        result.shouldBeEmpty()
    }

    "should delegate search to LLM with combined query" {
        val mockLLMService = mockk<LLMService>()
        val mockToolProvider = mockk<SearchToolProvider>()
        val searchService = LLMSearchService(mockLLMService, mockToolProvider)
        
        val expectedResult = listOf(
            LTMFile(
                frontmatter = LTMFrontmatter(
                    uuid = "test-uuid",
                    tags = listOf("test"),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    reinforcementCount = 1
                ),
                content = "Test content"
            )
        )
        
        every { 
            mockLLMService.searchWithTools(any(), mockToolProvider) 
        } returns expectedResult
        
        val queries = listOf("test query", "another query")
        val result = searchService.searchLTM(queries)
        
        result shouldBe expectedResult
        verify { mockLLMService.searchWithTools(any(), mockToolProvider) }
    }

    "should format queries properly for LLM" {
        val mockLLMService = mockk<LLMService>()
        val mockToolProvider = mockk<SearchToolProvider>()
        val searchService = LLMSearchService(mockLLMService, mockToolProvider)
        
        val querySlot = slot<String>()
        every { 
            mockLLMService.searchWithTools(capture(querySlot), mockToolProvider) 
        } returns emptyList()
        
        val queries = listOf("query one", "query two")
        searchService.searchLTM(queries)
        
        querySlot.captured shouldContain "1. query one"
        querySlot.captured shouldContain "2. query two"
        querySlot.captured shouldContain "Search the long-term memory"
    }
})