package com.brainiac.core.search

import com.brainiac.core.fileaccess.FileSystemService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import dev.mokkery.mock
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class LLMSearchServiceTest : StringSpec({

    "should return empty list for any query (stub implementation)" {
        val mockFileSystemService = mock<FileSystemService>()
        val fakeFileSystem = FakeFileSystem()
        val searchService = LLMSearchService(mockFileSystemService, "test/ltm".toPath(), fakeFileSystem)

        searchService.searchLTM("").shouldBeEmpty()
        searchService.searchLTM("test query").shouldBeEmpty()
        searchService.searchLTM("any other query").shouldBeEmpty()
    }
})