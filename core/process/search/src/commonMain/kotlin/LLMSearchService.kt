package com.brainiac.core.search

import com.brainiac.core.fileaccess.LTMFile
import com.brainiac.core.fileaccess.FileSystemService
import okio.Path
import okio.Path.Companion.toPath
import okio.FileSystem

class LLMSearchService(
    private val fileSystemService: FileSystemService,
    private val ltmRootPath: Path,
    private val fileSystem: FileSystem
) : SearchService {

    override fun searchLTM(query: String): List<LTMFile> {
        // TODO: Implement proper search logic without LLM abstraction
        return emptyList()
    }
}