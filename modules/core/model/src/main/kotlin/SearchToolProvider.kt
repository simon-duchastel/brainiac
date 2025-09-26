package com.brainiac.core.model

import java.nio.file.Path

interface SearchToolProvider {
    fun findAllLtmFiles(): List<Path>
    
    fun readLtmFile(path: Path): LTMFile?
    
    fun parseIndexFile(path: Path): IndexEntry?
    
    fun getFileMetadata(path: Path): FileMetadata?
    
    fun searchFileContent(path: Path, query: String): Boolean
    
    fun listDirectoryContents(path: Path): List<Path>
}

data class IndexEntry(
    val summary: String,
    val manifest: List<String>,
    val relatedMemories: List<String>
)

data class FileMetadata(
    val uuid: String,
    val tags: List<String>,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant,
    val reinforcementCount: Int
)