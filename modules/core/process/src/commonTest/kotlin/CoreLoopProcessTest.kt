package com.brainiac.core.process

import com.brainiac.core.fs.FileSystemService
import com.brainiac.core.llm.LLMService
import com.brainiac.core.search.SearchService
import com.brainiac.core.identity.CoreIdentityService
import com.brainiac.core.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.comparables.shouldBeLessThan
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlinx.datetime.Instant

class CoreLoopProcessTest : StringSpec({
    
    "should process user prompt with complete flow" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockLLMService = mock<LLMService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        
        val testStmContent = """# Short-Term Memory Scratchpad

## Summary
Recent conversation about programming concepts

---
## Structured Data

### Goals
- [ ] Learn Kotlin
- [ ] Build memory system

### Key Facts & Decisions
- User prefers functional programming

### Tasks
- [ ] Complete CoreLoopProcess implementation

---
## Event Log

### 2023-01-01T10:00:00Z
**User:** "What is Kotlin?"
**AI:** "Kotlin is a modern programming language..."
**Thoughts:** User asking about basics
"""
        
        val testLTMFile = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "kotlin-basics-001",
                createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2023-01-01T00:00:00Z"),
                tags = listOf("kotlin", "programming", "basics"),
                reinforcementCount = 5
            ),
            content = "# Kotlin Programming Language\n\nKotlin is a statically typed programming language..."
        )
        
        val coreIdentityContent = "# Core Identity\n\nI am Brainiac, an AI assistant designed to help with programming and learning."
        
        every { mockFileSystemService.readStm() } returns testStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns coreIdentityContent
        every { mockSearchService.searchLTM(any()) } returns listOf(testLTMFile)
        every { mockLLMService.generateResponse(any()) } returns "Based on the information provided, Kotlin is indeed a modern programming language..."
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            mockCoreIdentityService
        )
        
        val result = coreLoopProcess.processUserPrompt("Tell me more about Kotlin")
        
        result shouldBe "Based on the information provided, Kotlin is indeed a modern programming language..."
        
        verify { mockFileSystemService.readStm() }
        verify { mockCoreIdentityService.getCoreIdentityContent() }
        verify { mockSearchService.searchLTM(any()) }
        verify { mockLLMService.generateResponse(any()) }
    }
    
    "should handle empty STM gracefully" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockLLMService = mock<LLMService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        
        val emptyStmContent = ""
        
        val coreIdentityContent = "# Core Identity\n\nI am Brainiac."
        
        every { mockFileSystemService.readStm() } returns emptyStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns coreIdentityContent
        every { mockSearchService.searchLTM(any()) } returns emptyList()
        every { mockLLMService.generateResponse(any()) } returns "I understand your question."
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            mockCoreIdentityService
        )
        
        val result = coreLoopProcess.processUserPrompt("Hello")
        
        result shouldBe "I understand your question."
        
        verify { mockFileSystemService.readStm() }
        verify { mockSearchService.searchLTM(any()) }
    }
    
    "should include all STM components in initial context" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockLLMService = mock<LLMService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        
        val testStmContent = """# Short-Term Memory Scratchpad

## Summary
Learning about AI systems

---
## Structured Data

### Goals
- [ ] Understand memory systems

### Key Facts & Decisions
- AI needs persistent memory

### Tasks
- [ ] Read documentation
"""
        
        every { mockFileSystemService.readStm() } returns testStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns "Core identity"
        every { mockLLMService.generateResponse(any()) } returns "Response"
        
        var capturedSearchQuery = ""
        every { mockSearchService.searchLTM(any()) } calls { (query: String) ->
            capturedSearchQuery = query
            emptyList()
        }
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            mockCoreIdentityService
        )
        
        coreLoopProcess.processUserPrompt("Test prompt")
        
        // Verify that search is called with combined user prompt and initial context
        capturedSearchQuery shouldContain "Test prompt"
        capturedSearchQuery shouldContain "User Prompt: Test prompt"
        capturedSearchQuery shouldContain "Learning about AI systems"
    }
    
    "should format working memory correctly with LTM excerpts" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockLLMService = mock<LLMService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        
        val testStmContent = """# Short-Term Memory Scratchpad

## Summary
Test summary

---
## Structured Data

### Goals
- [ ] Test goal

### Key Facts & Decisions
- Test fact

### Tasks
- [ ] Test task

---
## Event Log

### 2023-01-01T10:00:00Z
**User:** "Hello"
**AI:** "Hi there"
**Thoughts:** Greeting
"""
        
        val testLTMFile = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "test-uuid",
                createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2023-01-01T01:00:00Z"),
                tags = listOf("test"),
                reinforcementCount = 1
            ),
            content = "Test LTM content"
        )
        
        every { mockFileSystemService.readStm() } returns testStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns "Test core identity"
        every { mockSearchService.searchLTM(any()) } returns listOf(testLTMFile)
        
        var capturedWorkingMemory = ""
        every { mockLLMService.generateResponse(any()) } calls { (workingMemory: String) ->
            capturedWorkingMemory = workingMemory
            "Response"
        }
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            mockCoreIdentityService
        )
        
        coreLoopProcess.processUserPrompt("Test")
        
        capturedWorkingMemory shouldContain "# Working Memory"
        capturedWorkingMemory shouldContain "## Core Identity"
        capturedWorkingMemory shouldContain "Test core identity"
        capturedWorkingMemory shouldContain "## User Prompt"
        capturedWorkingMemory shouldContain "Test"
        capturedWorkingMemory shouldContain "## Short-Term Memory"
        capturedWorkingMemory shouldContain "Test summary"
        capturedWorkingMemory shouldContain "## Relevant Long-Term Memory"
        capturedWorkingMemory shouldContain "**UUID:** test-uuid"
        capturedWorkingMemory shouldContain "Test LTM content"
        capturedWorkingMemory shouldContain "**User:** \"Hello\""
        capturedWorkingMemory shouldContain "**AI:** \"Hi there\""
        capturedWorkingMemory shouldContain "**Thoughts:** Greeting"
        
        // Verify correct ordering: Core Identity → STM → LTM → User Prompt
        val coreIdentityIndex = capturedWorkingMemory.indexOf("## Core Identity")
        val stmIndex = capturedWorkingMemory.indexOf("## Short-Term Memory")
        val ltmIndex = capturedWorkingMemory.indexOf("## Relevant Long-Term Memory")
        val promptIndex = capturedWorkingMemory.indexOf("## User Prompt")
        
        coreIdentityIndex shouldBeLessThan stmIndex
        stmIndex shouldBeLessThan ltmIndex
        ltmIndex shouldBeLessThan promptIndex
    }
    
    "should handle no LTM excerpts found" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockLLMService = mock<LLMService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        
        val testStmContent = """# Short-Term Memory Scratchpad

## Summary
Test summary
"""
        
        every { mockFileSystemService.readStm() } returns testStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns "Core identity"
        every { mockSearchService.searchLTM(any()) } returns emptyList()

        var capturedWorkingMemory = ""
        every { mockLLMService.generateResponse(any()) } calls { (workingMemory: String) ->
            capturedWorkingMemory = workingMemory
            "Response without LTM"
        }
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            mockCoreIdentityService
        )
        
        coreLoopProcess.processUserPrompt("Test")
        
        capturedWorkingMemory shouldContain "## Core Identity"
        capturedWorkingMemory shouldContain "## User Prompt"
        capturedWorkingMemory shouldContain "## Short-Term Memory"
        // Should not contain LTM section when no excerpts found
        capturedWorkingMemory.contains("## Relevant Long-Term Memory") shouldBe false
        
        // Verify correct ordering: Core Identity → STM → User Prompt (no LTM)
        val coreIdentityIndex = capturedWorkingMemory.indexOf("## Core Identity")
        val stmIndex = capturedWorkingMemory.indexOf("## Short-Term Memory")
        val promptIndex = capturedWorkingMemory.indexOf("## User Prompt")
        
        coreIdentityIndex shouldBeLessThan stmIndex
        stmIndex shouldBeLessThan promptIndex
    }
})