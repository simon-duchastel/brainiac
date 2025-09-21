package com.brainiac.core.process

import com.brainiac.core.fs.FileSystemService
import com.brainiac.core.llm.LLMService
import com.brainiac.core.search.SearchService
import com.brainiac.core.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Paths
import java.time.Instant

class CoreLoopProcessTest : StringSpec({
    
    "should process user prompt with complete flow" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        
        val testStm = ShortTermMemory(
            summary = "Recent conversation about programming concepts",
            structuredData = StructuredData(
                goals = listOf("Learn Kotlin", "Build memory system"),
                keyFacts = listOf("User prefers functional programming"),
                tasks = listOf("Complete CoreLoopProcess implementation")
            ),
            eventLog = listOf(
                Event(
                    timestamp = Instant.parse("2023-01-01T10:00:00Z"),
                    user = "What is Kotlin?",
                    ai = "Kotlin is a modern programming language...",
                    thoughts = "User asking about basics"
                )
            )
        )
        
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
        
        every { mockFileSystemService.readStm() } returns testStm
        every { mockFileSystemService.read(coreIdentityPath) } returns coreIdentityContent
        every { mockLLMService.generateSearchQueries(any(), any()) } returns listOf("kotlin programming", "language features")
        every { mockSearchService.searchLTM(any()) } returns listOf(testLTMFile)
        every { mockLLMService.generateResponse(any()) } returns "Based on the information provided, Kotlin is indeed a modern programming language..."
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            coreIdentityPath
        )
        
        val result = coreLoopProcess.processUserPrompt("Tell me more about Kotlin")
        
        result shouldBe "Based on the information provided, Kotlin is indeed a modern programming language..."
        
        verify { mockFileSystemService.readStm() }
        verify { mockFileSystemService.read(coreIdentityPath) }
        verify { mockLLMService.generateSearchQueries("Tell me more about Kotlin", any()) }
        verify { mockSearchService.searchLTM(listOf("kotlin programming", "language features")) }
        verify { mockLLMService.generateResponse(any()) }
    }
    
    "should handle empty STM gracefully" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        
        val emptyStm = ShortTermMemory(
            summary = "",
            structuredData = StructuredData(
                goals = emptyList(),
                keyFacts = emptyList(),
                tasks = emptyList()
            ),
            eventLog = emptyList()
        )
        
        val coreIdentityContent = "# Core Identity\n\nI am Brainiac."
        
        every { mockFileSystemService.readStm() } returns emptyStm
        every { mockFileSystemService.read(coreIdentityPath) } returns coreIdentityContent
        every { mockLLMService.generateSearchQueries(any(), any()) } returns listOf("general query")
        every { mockSearchService.searchLTM(any()) } returns emptyList()
        every { mockLLMService.generateResponse(any()) } returns "I understand your question."
        
        val coreLoopProcess = CoreLoopProcess(
            mockFileSystemService,
            mockLLMService,
            mockSearchService,
            coreIdentityPath
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
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        
        val testStm = ShortTermMemory(
            summary = "Learning about AI systems",
            structuredData = StructuredData(
                goals = listOf("Understand memory systems"),
                keyFacts = listOf("AI needs persistent memory"),
                tasks = listOf("Read documentation")
            ),
            eventLog = emptyList()
        )
        
        every { mockFileSystemService.readStm() } returns testStm
        every { mockFileSystemService.read(coreIdentityPath) } returns "Core identity"
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
            coreIdentityPath
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
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        
        val testStm = ShortTermMemory(
            summary = "Test summary",
            structuredData = StructuredData(
                goals = listOf("Test goal"),
                keyFacts = listOf("Test fact"),
                tasks = listOf("Test task")
            ),
            eventLog = listOf(
                Event(
                    timestamp = Instant.parse("2023-01-01T10:00:00Z"),
                    user = "Hello",
                    ai = "Hi there",
                    thoughts = "Greeting"
                )
            )
        )
        
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
        
        every { mockFileSystemService.readStm() } returns testStm
        every { mockFileSystemService.read(coreIdentityPath) } returns "Test core identity"
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
            coreIdentityPath
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
    }
    
    "should handle no LTM excerpts found" {
        val mockFileSystemService = mockk<FileSystemService>()
        val mockLLMService = mockk<LLMService>()
        val mockSearchService = mockk<SearchService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        
        val testStm = ShortTermMemory(
            summary = "Test summary",
            structuredData = StructuredData(
                goals = emptyList(),
                keyFacts = emptyList(),
                tasks = emptyList()
            ),
            eventLog = emptyList()
        )
        
        every { mockFileSystemService.readStm() } returns testStm
        every { mockFileSystemService.read(coreIdentityPath) } returns "Core identity"
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
            coreIdentityPath
        )
        
        coreLoopProcess.processUserPrompt("Test")
        
        capturedWorkingMemory shouldContain "## Core Identity"
        capturedWorkingMemory shouldContain "## User Prompt"
        capturedWorkingMemory shouldContain "## Short-Term Memory"
        // Should not contain LTM section when no excerpts found
        capturedWorkingMemory.contains("## Relevant Long-Term Memory") shouldBe false
    }
})