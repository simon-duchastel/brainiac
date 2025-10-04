package com.duchastel.simon.brainiac.core.process

import com.duchastel.simon.brainiac.core.fileaccess.FileSystemService
import com.duchastel.simon.brainiac.core.search.SearchService
import com.duchastel.simon.brainiac.core.identity.CoreIdentityService
import com.duchastel.simon.brainiac.core.fileaccess.*
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
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CoreLoopProcessTest : StringSpec({
    
    "should process user prompt with complete flow" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        val mockModelProvider = mock<ModelProvider>()

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

        // Mock modelProvider to return the working memory unchanged so we can verify it
        every { mockModelProvider.process(any()) } calls { (input: String) -> input }

        val coreLoopProcess = DefaultCoreLoopProcess(
            mockFileSystemService,
            mockSearchService,
            mockCoreIdentityService,
            mockModelProvider
        )

        val result = coreLoopProcess.processUserPrompt("Tell me more about Kotlin")

        result shouldContain "# Working Memory"
        result shouldContain coreIdentityContent
        result shouldContain testStmContent
        result shouldContain "kotlin-basics-001"
        result shouldContain "Tell me more about Kotlin"

        verify { mockFileSystemService.readStm() }
        verify { mockCoreIdentityService.getCoreIdentityContent() }
        verify { mockSearchService.searchLTM(any()) }
    }
    
    "should handle empty STM gracefully" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        val mockModelProvider = mock<ModelProvider>()

        val emptyStmContent = ""

        val coreIdentityContent = "# Core Identity\n\nI am Brainiac."

        every { mockFileSystemService.readStm() } returns emptyStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns coreIdentityContent
        every { mockSearchService.searchLTM(any()) } returns emptyList()

        // Mock modelProvider to return the working memory unchanged so we can verify it
        every { mockModelProvider.process(any()) } calls { (input: String) -> input }

        val coreLoopProcess = DefaultCoreLoopProcess(
            mockFileSystemService,
            mockSearchService,
            mockCoreIdentityService,
            mockModelProvider
        )

        val result = coreLoopProcess.processUserPrompt("Hello")

        result shouldContain "# Working Memory"
        result shouldContain coreIdentityContent
        result shouldContain "Hello"

        verify { mockFileSystemService.readStm() }
        verify { mockSearchService.searchLTM(any()) }
    }
    
    "should include all STM components in initial context" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        val mockModelProvider = mock<ModelProvider>()

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

        // Mock modelProvider to return empty string for this test
        every { mockModelProvider.process(any()) } returns ""

        var capturedSearchQuery = ""
        every { mockSearchService.searchLTM(any()) } calls { (query: String) ->
            capturedSearchQuery = query
            emptyList()
        }

        val coreLoopProcess = DefaultCoreLoopProcess(
            mockFileSystemService,
            mockSearchService,
            mockCoreIdentityService,
            mockModelProvider
        )

        coreLoopProcess.processUserPrompt("Test prompt")

        // Verify that search is called with combined user prompt and initial context
        capturedSearchQuery shouldContain "Test prompt"
        capturedSearchQuery shouldContain "User Prompt: Test prompt"
        capturedSearchQuery shouldContain "Learning about AI systems"
    }
    
    "should format working memory correctly with LTM excerpts" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        val mockModelProvider = mock<ModelProvider>()

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

        // Mock modelProvider to return the working memory unchanged so we can verify it
        every { mockModelProvider.process(any()) } calls { (input: String) -> input }

        val coreLoopProcess = DefaultCoreLoopProcess(
            mockFileSystemService,
            mockSearchService,
            mockCoreIdentityService,
            mockModelProvider
        )

        val workingMemory = coreLoopProcess.processUserPrompt("Test")

        workingMemory shouldContain "# Working Memory"
        workingMemory shouldContain "## Core Identity"
        workingMemory shouldContain "Test core identity"
        workingMemory shouldContain "## User Prompt"
        workingMemory shouldContain "Test"
        workingMemory shouldContain "## Short-Term Memory"
        workingMemory shouldContain "Test summary"
        workingMemory shouldContain "## Relevant Long-Term Memory"
        workingMemory shouldContain "**UUID:** test-uuid"
        workingMemory shouldContain "Test LTM content"
        workingMemory shouldContain "**User:** \"Hello\""
        workingMemory shouldContain "**AI:** \"Hi there\""
        workingMemory shouldContain "**Thoughts:** Greeting"

        // Verify correct ordering: Core Identity → STM → LTM → User Prompt
        val coreIdentityIndex = workingMemory.indexOf("## Core Identity")
        val stmIndex = workingMemory.indexOf("## Short-Term Memory")
        val ltmIndex = workingMemory.indexOf("## Relevant Long-Term Memory")
        val promptIndex = workingMemory.indexOf("## User Prompt")

        coreIdentityIndex shouldBeLessThan stmIndex
        stmIndex shouldBeLessThan ltmIndex
        ltmIndex shouldBeLessThan promptIndex
    }
    
    "should handle no LTM excerpts found" {
        val mockFileSystemService = mock<FileSystemService>()
        val mockSearchService = mock<SearchService>()
        val mockCoreIdentityService = mock<CoreIdentityService>()
        val mockModelProvider = mock<ModelProvider>()

        val testStmContent = """# Short-Term Memory Scratchpad

## Summary
Test summary
"""

        every { mockFileSystemService.readStm() } returns testStmContent
        every { mockCoreIdentityService.getCoreIdentityContent() } returns "Core identity"
        every { mockSearchService.searchLTM(any()) } returns emptyList()

        // Mock modelProvider to return the working memory unchanged so we can verify it
        every { mockModelProvider.process(any()) } calls { (input: String) -> input }

        val coreLoopProcess = DefaultCoreLoopProcess(
            mockFileSystemService,
            mockSearchService,
            mockCoreIdentityService,
            mockModelProvider
        )

        val workingMemory = coreLoopProcess.processUserPrompt("Test")

        workingMemory shouldContain "## Core Identity"
        workingMemory shouldContain "## User Prompt"
        workingMemory shouldContain "## Short-Term Memory"
        // Should not contain LTM section when no excerpts found
        workingMemory.contains("## Relevant Long-Term Memory") shouldBe false

        // Verify correct ordering: Core Identity → STM → User Prompt (no LTM)
        val coreIdentityIndex = workingMemory.indexOf("## Core Identity")
        val stmIndex = workingMemory.indexOf("## Short-Term Memory")
        val promptIndex = workingMemory.indexOf("## User Prompt")

        coreIdentityIndex shouldBeLessThan stmIndex
        stmIndex shouldBeLessThan promptIndex
    }
})