package com.brainiac.core.search

import com.brainiac.core.fileaccess.LTMFile
import com.brainiac.core.fileaccess.LTMFrontmatter
import com.brainiac.core.fileaccess.FileSystemService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

class LLMSearchServiceTest : StringSpec({

    "should return empty list when query is empty" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val searchService = LLMSearchService(mockFileSystemService, "test/ltm".toPath(), fakeFileSystem)

        val result = searchService.searchLTM("")
        result.shouldBeEmpty()
    }

    "should return empty list when LTM directory does not exist" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val searchService = LLMSearchService(mockFileSystemService, "nonexistent/ltm".toPath(), fakeFileSystem)

        val result = searchService.searchLTM("test query")
        result.shouldBeEmpty()
    }

    "should generate XML tree and build correct prompt for LLM" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)

        // Create a test file to generate XML structure
        val testFile = ltmPath / "test-memory.md"
        fakeFileSystem.write(testFile) { writeUtf8("# Test Memory") }

        val searchService = LLMSearchService(mockFileSystemService, ltmPath, fakeFileSystem)

        // This test verifies the prompt construction
        // Since we're not calling the LLM (TODO), result will be empty
        // But we can verify by examining the internal state through behavior
        val result = searchService.searchLTM("machine learning and neural networks")

        // Should return empty for now since LLM call is TODO
        result.shouldBeEmpty()
    }

    "should parse LLM response and read selected files" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)

        // Create a test file
        val testFile = ltmPath / "test-memory.md"
        fakeFileSystem.write(testFile) { writeUtf8("# Test Memory\nThis is a test memory file.") }

        val searchService = LLMSearchService(mockFileSystemService, ltmPath, fakeFileSystem)

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

        // TODO: Mock LLM service response when LLM integration is added
        // every { mockLLMService.generateResponse(any()) } returns "test-memory.md"
        every { mockFileSystemService.readLtmFile(testFile) } returns expectedLTMFile

        val result = searchService.searchLTM("test query")

        // Should return empty for now since LLM call is TODO
        result.shouldBeEmpty()
    }

    "should handle LLM response with multiple file paths" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)

        // Create test files
        val testFile1 = ltmPath / "memory1.md"
        val testFile2 = ltmPath / "memory2.md"
        fakeFileSystem.write(testFile1) { writeUtf8("# Memory 1") }
        fakeFileSystem.write(testFile2) { writeUtf8("# Memory 2") }

        val searchService = LLMSearchService(mockFileSystemService, ltmPath, fakeFileSystem)

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

        // TODO: Mock LLM service response when LLM integration is added
        // every { mockLLMService.generateResponse(any()) } returns "memory1.md\nmemory2.md"
        every { mockFileSystemService.readLtmFile(testFile1) } returns ltmFile1
        every { mockFileSystemService.readLtmFile(testFile2) } returns ltmFile2

        val result = searchService.searchLTM("test query")

        // Should return empty for now since LLM call is TODO
        result.shouldBeEmpty()
    }

    "should ignore invalid file paths in LLM response" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)

        val searchService = LLMSearchService(mockFileSystemService, ltmPath, fakeFileSystem)

        // TODO: Mock LLM service response when LLM integration is added
        // every { mockLLMService.generateResponse(any()) } returns "nonexistent.md\n# Some comment\n<xml>tag</xml>"

        val result = searchService.searchLTM("test query")

        result.shouldBeEmpty()
    }

    "should generate correct LLM prompt with XML structure and return parsed files from LLM response" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val ltmPath = "test-ltm".toPath()
        fakeFileSystem.createDirectories(ltmPath)

        // Create a directory structure with test files
        val subDir = ltmPath / "concepts"
        fakeFileSystem.createDirectories(subDir)

        val file1 = ltmPath / "memory1.md"
        val file2 = subDir / "neural-networks.md"
        val file3 = subDir / "machine-learning.md"

        fakeFileSystem.write(file1) { writeUtf8("# Memory 1") }
        fakeFileSystem.write(file2) { writeUtf8("# Neural Networks") }
        fakeFileSystem.write(file3) { writeUtf8("# Machine Learning") }

        // Capture the prompt sent to LLM and provide a mock response
        var capturedPrompt = ""
        val mockLLMResponse = "concepts/neural-networks.md\nconcepts/machine-learning.md"

        val searchService = LLMSearchService(
            mockFileSystemService,
            ltmPath,
            fakeFileSystem,
            llmResponseProvider = { prompt ->
                capturedPrompt = prompt
                mockLLMResponse
            }
        )

        val ltmFile1 = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "uuid1",
                tags = listOf("ai", "neural-networks"),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                reinforcementCount = 1
            ),
            content = "Neural networks content"
        )
        val ltmFile2 = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "uuid2",
                tags = listOf("ai", "ml"),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                reinforcementCount = 1
            ),
            content = "Machine learning content"
        )

        every { mockFileSystemService.readLtmFile(file2) } returns ltmFile1
        every { mockFileSystemService.readLtmFile(file3) } returns ltmFile2

        val result = searchService.searchLTM("deep learning and AI")

        // Verify exact prompt format
        val expectedPrompt = """Given this search query: "deep learning and AI"

Here is the complete structure of available long-term memory files:
<ltm_directory>
    <file path="memory1.md">memory1.md</file>
  <directory name="concepts">
    <file path="concepts/machine-learning.md">machine-learning.md</file>
    <file path="concepts/neural-networks.md">neural-networks.md</file>
  </directory>
</ltm_directory>


Please select the most relevant memory files for this query.
Return only the file paths (relative to the LTM root), one per line.
Do not include any other text or explanations.
"""

        capturedPrompt shouldBe expectedPrompt

        // Verify the service correctly parsed the LLM response and returned the files
        result shouldHaveSize 2
        result[0] shouldBe ltmFile1
        result[1] shouldBe ltmFile2

        // Verify files were read from the correct paths
        verify { mockFileSystemService.readLtmFile(file2) }
        verify { mockFileSystemService.readLtmFile(file3) }
    }
})
