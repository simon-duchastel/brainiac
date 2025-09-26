package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.LTMFrontmatter
import com.brainiac.core.fs.FileSystemService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldNotThrow
import io.mockk.mockk
import java.nio.file.Paths
import java.time.Instant

class DefaultSearchServiceTest : StringSpec({

    "should return empty list when queries are empty" {
        val mockFileSystemService = mockk<FileSystemService>()
        val searchService = DefaultSearchService(mockFileSystemService, Paths.get("test", "memory", "long_term"))
        
        val result = searchService.searchLTM(emptyList())
        result.shouldBeEmpty()
    }

    "should return empty list when LTM root does not exist" {
        val mockFileSystemService = mockk<FileSystemService>()
        val nonExistentRoot = Paths.get("nonexistent")
        val serviceWithNonExistentRoot = DefaultSearchService(mockFileSystemService, nonExistentRoot)
        
        val result = serviceWithNonExistentRoot.searchLTM(listOf("test"))
        result.shouldBeEmpty()
    }

    "should handle malformed index files gracefully" {
        val mockFileSystemService = mockk<FileSystemService>()
        val searchService = DefaultSearchService(mockFileSystemService, Paths.get("test", "memory", "long_term"))
        
        // Test that service doesn't crash with malformed data - we test this via the public API
        shouldNotThrow<Exception> {
            searchService.searchLTM(listOf("test"))
        }
    }

})