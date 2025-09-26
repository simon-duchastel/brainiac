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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Instant

class CoreLoopProcessTest : StringSpec({
    
    "should process user prompt with complete flow" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val mockCoreIdentityService = mockk<CoreIdentityService>()
        
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
        every { mockLLMService.generateSearchQueries(any(), any()) } returns listOf("kotlin programming", "language features")
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
        verify { mockLLMService.generateSearchQueries("Tell me more about Kotlin", any()) }
        verify { mockSearchService.searchLTM(listOf("kotlin programming", "language features")) }
        verify { mockLLMService.generateResponse(any()) }
    }
    
    "should handle empty STM gracefully" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val mockCoreIdentityService = mockk<CoreIdentityService>()
        
        val emptyStmContent = ""
        
        val coreIdentityContent = "# Core Identity\n\nI am Brainiac."
        
        every { mockFileSystemService.readStm() } returns emptyStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns coreIdentityContent
        every { mockLLMService.generateSearchQueries(any(), any()) } returns listOf("general query")
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
        verify { mockLLMService.generateSearchQueries("Hello", any()) }
        verify { mockSearchService.searchLTM(listOf("general query")) }
    }
    
    "should include all STM components in initial context" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val mockCoreIdentityService = mockk<CoreIdentityService>()
        
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
        every { mockSearchService.searchLTM(any()) } returns emptyList()
        every { mockLLMService.generateResponse(any()) } returns "Response"
        
        var capturedContext = ""
        every { mockLLMService.generateSearchQueries(any(), any()) } answers {
            capturedContext = secondArg()
            listOf("test query")
        }
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            mockCoreIdentityService
        )
        
        coreLoopProcess.processUserPrompt("Test prompt")
        
        capturedContext shouldContain "User Prompt: Test prompt"
        capturedContext shouldContain "Learning about AI systems"
        capturedContext shouldContain "Understand memory systems"
        capturedContext shouldContain "AI needs persistent memory"
        capturedContext shouldContain "Read documentation"
    }
    
    "should format working memory correctly with LTM excerpts" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val mockCoreIdentityService = mockk<CoreIdentityService>()
        
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
        every { mockLLMService.generateSearchQueries(any(), any()) } returns listOf("test")
        every { mockSearchService.searchLTM(any()) } returns listOf(testLTMFile)
        
        var capturedWorkingMemory = ""
        every { mockLLMService.generateResponse(any()) } answers {
            capturedWorkingMemory = firstArg()
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
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val mockCoreIdentityService = mockk<CoreIdentityService>()
        
        val testStmContent = """# Short-Term Memory Scratchpad

## Summary
Test summary
"""
        
        every { mockFileSystemService.readStm() } returns testStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns "Core identity"
        every { mockLLMService.generateSearchQueries(any(), any()) } returns listOf("no results")
        every { mockSearchService.searchLTM(any()) } returns emptyList()
        
        var capturedWorkingMemory = ""
        every { mockLLMService.generateResponse(any()) } answers {
            capturedWorkingMemory = firstArg()
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