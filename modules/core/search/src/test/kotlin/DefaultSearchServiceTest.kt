package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.LTMFrontmatter
import com.brainiac.core.fs.FileSystemService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

class DefaultSearchServiceTest {

    private lateinit var mockFileSystemService: FileSystemService
    private lateinit var searchService: DefaultSearchService
    private val testLtmRoot = Paths.get("test", "memory", "long_term")

    @BeforeEach
    fun setUp() {
        mockFileSystemService = mockk()
        searchService = DefaultSearchService(mockFileSystemService, testLtmRoot)
    }

    @Test
    fun `should return empty list when queries are empty`() {
        val result = searchService.searchLTM(emptyList())
        assertEquals(emptyList<LTMFile>(), result)
    }

    @Test
    fun `should return empty list when LTM root does not exist`() {
        // FileSystem operations will be mocked, but we can test this via the directory not existing
        val nonExistentRoot = Paths.get("nonexistent")
        val serviceWithNonExistentRoot = DefaultSearchService(mockFileSystemService, nonExistentRoot)
        
        val result = serviceWithNonExistentRoot.searchLTM(listOf("test"))
        assertEquals(emptyList<LTMFile>(), result)
    }

    @Test
    fun `should parse index file correctly`() {
        val indexContent = """
            # Programming Concepts
            
            ## Summary
            This directory contains various programming concepts and techniques.
            
            ## Manifest
            - python_basics.md
            - algorithms.md
            - data_structures.md
            
            ## Related Memories
            - [Object-Oriented Programming](123e4567-e89b-12d3-a456-426614174000)
            - [Functional Programming](456e7890-e12b-34d5-a678-901234567890)
        """.trimIndent()

        // Use reflection to access private method for testing
        val method = DefaultSearchService::class.java.getDeclaredMethod("parseIndexFile", String::class.java)
        method.isAccessible = true
        val result = method.invoke(searchService, indexContent) as DefaultSearchService.IndexEntry

        assertEquals("This directory contains various programming concepts and techniques.", result.summary)
        assertEquals(listOf("python_basics.md", "algorithms.md", "data_structures.md"), result.manifest)
        assertEquals(listOf("123e4567-e89b-12d3-a456-426614174000", "456e7890-e12b-34d5-a678-901234567890"), result.relatedMemories)
    }

    @Test
    fun `should calculate relevance score based on content matching`() {
        val ltmFile = createTestLTMFile(
            uuid = "test-uuid",
            content = "This is a test document about Python programming",
            tags = listOf("programming", "python"),
            reinforcementCount = 5
        )

        // Mock file operations to simulate file existence and content
        every { mockFileSystemService.readLtmFile(any()) } returns ltmFile

        // Use reflection to test calculateRelevanceScore
        val method = DefaultSearchService::class.java.getDeclaredMethod(
            "calculateRelevanceScore", 
            LTMFile::class.java,
            Path::class.java,
            List::class.java,
            Map::class.java
        )
        method.isAccessible = true
        
        val filePath = Paths.get("test", "file.md")
        val queries = listOf("python", "programming")
        val indexEntries = emptyMap<Path, DefaultSearchService.IndexEntry>()
        
        val score = method.invoke(searchService, ltmFile, filePath, queries, indexEntries) as Double
        
        // Should score for content matching + tag matching + reinforcement count
        assertTrue(score > 0.0, "Score should be greater than 0 for matching content and tags")
        assertTrue(score > 5.0, "Score should account for content match, tag matches, and reinforcement")
    }

    @Test
    fun `should prioritize tag matches over content matches`() {
        val fileWithTagMatch = createTestLTMFile(
            uuid = "tag-match",
            content = "Some unrelated content",
            tags = listOf("kotlin"),
            reinforcementCount = 0
        )

        val fileWithContentMatch = createTestLTMFile(
            uuid = "content-match", 
            content = "This document talks about kotlin programming",
            tags = listOf("programming"),
            reinforcementCount = 0
        )

        val method = DefaultSearchService::class.java.getDeclaredMethod(
            "calculateRelevanceScore",
            LTMFile::class.java,
            Path::class.java,
            List::class.java,
            Map::class.java
        )
        method.isAccessible = true

        val filePath = Paths.get("test", "file.md")
        val queries = listOf("kotlin")
        val indexEntries = emptyMap<Path, DefaultSearchService.IndexEntry>()

        val tagScore = method.invoke(searchService, fileWithTagMatch, filePath, queries, indexEntries) as Double
        val contentScore = method.invoke(searchService, fileWithContentMatch, filePath, queries, indexEntries) as Double

        assertTrue(tagScore > contentScore, "Tag matches should have higher scores than content matches")
    }

    @Test
    fun `should give recency bonus to recently updated files`() {
        val recentFile = createTestLTMFile(
            uuid = "recent",
            content = "test content",
            tags = emptyList(),
            reinforcementCount = 0,
            updatedAt = Instant.now().minusSeconds(3600) // 1 hour ago
        )

        val oldFile = createTestLTMFile(
            uuid = "old",
            content = "test content", 
            tags = emptyList(),
            reinforcementCount = 0,
            updatedAt = Instant.now().minusSeconds(86400 * 60) // 60 days ago
        )

        val method = DefaultSearchService::class.java.getDeclaredMethod(
            "calculateRelevanceScore",
            LTMFile::class.java,
            Path::class.java,
            List::class.java,
            Map::class.java
        )
        method.isAccessible = true

        val filePath = Paths.get("test", "file.md")
        val queries = listOf("test")
        val indexEntries = emptyMap<Path, DefaultSearchService.IndexEntry>()

        val recentScore = method.invoke(searchService, recentFile, filePath, queries, indexEntries) as Double
        val oldScore = method.invoke(searchService, oldFile, filePath, queries, indexEntries) as Double

        assertTrue(recentScore > oldScore, "Recently updated files should have higher scores")
    }

    @Test
    fun `should handle malformed index files gracefully`() {
        val malformedIndexContent = """
            This is not a properly formatted index file
            No proper sections or structure
        """.trimIndent()

        val method = DefaultSearchService::class.java.getDeclaredMethod("parseIndexFile", String::class.java)
        method.isAccessible = true
        
        // Should not throw exception and return reasonable defaults
        assertDoesNotThrow {
            val result = method.invoke(searchService, malformedIndexContent) as DefaultSearchService.IndexEntry
            assertEquals("", result.summary)
            assertEquals(emptyList<String>(), result.manifest)
            assertEquals(emptyList<String>(), result.relatedMemories)
        }
    }

    private fun createTestLTMFile(
        uuid: String,
        content: String,
        tags: List<String>,
        reinforcementCount: Int,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): LTMFile {
        val frontmatter = LTMFrontmatter(
            uuid = uuid,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tags = tags,
            reinforcementCount = reinforcementCount
        )
        return LTMFile(frontmatter, content)
    }
}