package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.LTMFrontmatter
import com.brainiac.core.llm.LLMService
import com.brainiac.core.fs.FileSystemService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.slot
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

class LLMSearchServiceTest : StringSpec({

    "should return empty list when query is empty" {
        val mockLLMService = mockk<LLMService>()
        val mockFileSystemService = mockk<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val searchService = LLMSearchService(mockLLMService, mockFileSystemService, "test/ltm".toPath(), fakeFileSystem)
        
        val result = searchService.searchLTM("")
        result.shouldBeEmpty()
    }

    "should return empty list when LTM directory does not exist" {
        val mockLLMService = mockk<LLMService>()
        val mockFileSystemService = mockk<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val searchService = LLMSearchService(mockLLMService, mockFileSystemService, "nonexistent/ltm".toPath(), fakeFileSystem)
        
        val result = searchService.searchLTM("test query")
        result.shouldBeEmpty()
    }

    "should include query and XML structure in LLM prompt" {
        val mockLLMService = mockk<LLMService>()
        val mockFileSystemService = mockk<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)
        val searchService = LLMSearchService(mockLLMService, mockFileSystemService, ltmPath, fakeFileSystem)
        
        val promptSlot = slot<String>()
        every { mockLLMService.generateResponse(capture(promptSlot)) } returns ""
        
        val query = "machine learning and neural networks"
        searchService.searchLTM(query)
        
        promptSlot.captured shouldContain "machine learning and neural networks"
        promptSlot.captured shouldContain "<ltm_directory>"
        promptSlot.captured shouldContain "Please select the most relevant memory files"
    }

    "should parse LLM response and read selected files" {
        val mockLLMService = mockk<LLMService>()
        val mockFileSystemService = mockk<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)
        
        // Create a test file
        val testFile = ltmPath / "test-memory.md"
        fakeFileSystem.write(testFile) { writeUtf8("# Test Memory\nThis is a test memory file.") }
        
        val searchService = LLMSearchService(mockLLMService, mockFileSystemService, ltmPath, fakeFileSystem)
        
        val expectedLTMFile = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "test-uuid",
                tags = listOf("test"),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                reinforcementCount = 1
            ),
            content = "Test content"
        )
        
        every { mockLLMService.generateResponse(any()) } returns "test-memory.md"
        every { mockFileSystemService.readLtmFile(testFile) } returns expectedLTMFile
        
        val result = searchService.searchLTM("test query")
        
        result shouldHaveSize 1
        result[0] shouldBe expectedLTMFile
        verify { mockFileSystemService.readLtmFile(testFile) }
    }

    "should handle LLM response with multiple file paths" {
        val mockLLMService = mockk<LLMService>()
        val mockFileSystemService = mockk<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)
        
        // Create test files
        val testFile1 = ltmPath / "memory1.md"
        val testFile2 = ltmPath / "memory2.md"
        fakeFileSystem.write(testFile1) { writeUtf8("# Memory 1") }
        fakeFileSystem.write(testFile2) { writeUtf8("# Memory 2") }
        
        val searchService = LLMSearchService(mockLLMService, mockFileSystemService, ltmPath, fakeFileSystem)
        
        val ltmFile1 = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "uuid1",
                tags = listOf("tag1"),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                reinforcementCount = 1
            ),
            content = "Content 1"
        )
        val ltmFile2 = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "uuid2",
                tags = listOf("tag2"),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                reinforcementCount = 1
            ),
            content = "Content 2"
        )
        
        every { mockLLMService.generateResponse(any()) } returns "memory1.md\nmemory2.md"
        every { mockFileSystemService.readLtmFile(testFile1) } returns ltmFile1
        every { mockFileSystemService.readLtmFile(testFile2) } returns ltmFile2
        
        val result = searchService.searchLTM("test query")
        
        result shouldHaveSize 2
        result[0] shouldBe ltmFile1
        result[1] shouldBe ltmFile2
    }

    "should ignore invalid file paths in LLM response" {
        val mockLLMService = mockk<LLMService>()
        val mockFileSystemService = mockk<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)
        
        val searchService = LLMSearchService(mockLLMService, mockFileSystemService, ltmPath, fakeFileSystem)
        
        every { mockLLMService.generateResponse(any()) } returns "nonexistent.md\n# Some comment\n<xml>tag</xml>"
        
        val result = searchService.searchLTM("test query")
        
        result.shouldBeEmpty()
    }
})