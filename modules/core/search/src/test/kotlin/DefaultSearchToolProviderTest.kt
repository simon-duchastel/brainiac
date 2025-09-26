package com.brainiac.core.search

import com.brainiac.core.fs.FileSystemService
import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.LTMFrontmatter
import com.brainiac.core.model.SearchToolProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.mockk
import io.mockk.every
import java.nio.file.Paths
import java.time.Instant

class DefaultSearchToolProviderTest : StringSpec({

    "should return empty list when LTM root does not exist" {
        val mockFileSystemService = mockk<FileSystemService>()
        val nonExistentRoot = Paths.get("nonexistent")
        val toolProvider = DefaultSearchToolProvider(mockFileSystemService, nonExistentRoot)
        
        val result = toolProvider.findAllLtmFiles()
        result.shouldBeEmpty()
    }

    "should return null when reading non-existent LTM file" {
        val mockFileSystemService = mockk<FileSystemService>()
        val toolProvider = DefaultSearchToolProvider(mockFileSystemService)
        
        every { mockFileSystemService.readLtmFile(any()) } throws Exception("File not found")
        
        val result = toolProvider.readLtmFile(Paths.get("nonexistent.md"))
        result.shouldBeNull()
    }

    "should successfully read LTM file when it exists" {
        val mockFileSystemService = mockk<FileSystemService>()
        val toolProvider = DefaultSearchToolProvider(mockFileSystemService)
        
        val expectedFile = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "test-uuid",
                tags = listOf("test"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                reinforcementCount = 1
            ),
            content = "Test content"
        )
        
        every { mockFileSystemService.readLtmFile(any()) } returns expectedFile
        
        val result = toolProvider.readLtmFile(Paths.get("test.md"))
        result.shouldNotBeNull()
        result shouldBe expectedFile
    }

    "should extract metadata from LTM file" {
        val mockFileSystemService = mockk<FileSystemService>()
        val toolProvider = DefaultSearchToolProvider(mockFileSystemService)
        
        val ltmFile = LTMFile(
            frontmatter = LTMFrontmatter(
                uuid = "test-uuid",
                tags = listOf("tag1", "tag2"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                reinforcementCount = 5
            ),
            content = "Test content"
        )
        
        every { mockFileSystemService.readLtmFile(any()) } returns ltmFile
        
        val result = toolProvider.getFileMetadata(Paths.get("test.md"))
        result.shouldNotBeNull()
        result.uuid shouldBe "test-uuid"
        result.tags shouldBe listOf("tag1", "tag2")
        result.reinforcementCount shouldBe 5
    }

    "should search file content case-insensitively" {
        val mockFileSystemService = mockk<FileSystemService>()
        val toolProvider = DefaultSearchToolProvider(mockFileSystemService)
        
        every { mockFileSystemService.read(any()) } returns "This is TEST content"
        
        val result1 = toolProvider.searchFileContent(Paths.get("test.md"), "test")
        val result2 = toolProvider.searchFileContent(Paths.get("test.md"), "TEST")
        val result3 = toolProvider.searchFileContent(Paths.get("test.md"), "missing")
        
        result1 shouldBe true
        result2 shouldBe true
        result3 shouldBe false
    }

    "should parse index file with summary, manifest, and related memories" {
        val mockFileSystemService = mockk<FileSystemService>()
        val toolProvider = DefaultSearchToolProvider(mockFileSystemService)
        
        val indexContent = """
        # Summary
        This is a test directory summary.
        
        ## Manifest
        - file1.md
        - file2.md
        
        ## Related Memories
        - [Related Memory 1](uuid-1)
        - [Related Memory 2](uuid-2)
        """.trimIndent()
        
        every { mockFileSystemService.read(any()) } returns indexContent
        
        val result = toolProvider.parseIndexFile(Paths.get("_index.md"))
        result.shouldNotBeNull()
        result.summary shouldBe "This is a test directory summary."
        result.manifest shouldBe listOf("file1.md", "file2.md")
        result.relatedMemories shouldBe listOf("uuid-1", "uuid-2")
    }
})