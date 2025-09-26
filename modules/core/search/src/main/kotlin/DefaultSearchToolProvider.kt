package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.model.SearchToolProvider
import com.brainiac.core.model.IndexEntry
import com.brainiac.core.model.FileMetadata
import com.brainiac.core.fs.FileSystemService
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import kotlin.streams.toList

class DefaultSearchToolProvider(
    private val fileSystemService: FileSystemService,
    private val ltmRootPath: Path = Paths.get("memory", "long_term")
) : SearchToolProvider {

    override fun findAllLtmFiles(): List<Path> {
        if (!Files.exists(ltmRootPath)) {
            return emptyList()
        }

        return Files.walk(ltmRootPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(".md") }
                .filter { !it.fileName.toString().startsWith("_index") }
                .toList()
        }
    }

    override fun readLtmFile(path: Path): LTMFile? {
        return try {
            fileSystemService.readLtmFile(path)
        } catch (e: Exception) {
            null
        }
    }

    override fun parseIndexFile(path: Path): IndexEntry? {
        return try {
            val content = fileSystemService.read(path)
            parseIndexFileContent(content)
        } catch (e: Exception) {
            null
        }
    }

    override fun getFileMetadata(path: Path): FileMetadata? {
        return try {
            val ltmFile = fileSystemService.readLtmFile(path)
            FileMetadata(
                uuid = ltmFile.frontmatter.uuid,
                tags = ltmFile.frontmatter.tags,
                createdAt = ltmFile.frontmatter.createdAt,
                updatedAt = ltmFile.frontmatter.updatedAt,
                reinforcementCount = ltmFile.frontmatter.reinforcementCount
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun searchFileContent(path: Path, query: String): Boolean {
        return try {
            val content = fileSystemService.read(path)
            content.lowercase().contains(query.lowercase())
        } catch (e: Exception) {
            false
        }
    }

    override fun listDirectoryContents(path: Path): List<Path> {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return emptyList()
        }

        return Files.list(path).use { stream ->
            stream.toList()
        }
    }

    private fun parseIndexFileContent(content: String): IndexEntry {
        val lines = content.lines()
        var summary = ""
        val manifest = mutableListOf<String>()
        val relatedMemories = mutableListOf<String>()
        
        var currentSection = ""
        val summaryBuilder = StringBuilder()
        
        for (line in lines) {
            when {
                line.startsWith("# ") || line.startsWith("## ") -> {
                    if (currentSection == "summary" && summaryBuilder.isNotEmpty()) {
                        summary = summaryBuilder.toString().trim()
                    }
                    when {
                        line.lowercase().contains("summary") -> {
                            currentSection = "summary"
                            summaryBuilder.clear()
                        }
                        line.lowercase().contains("manifest") -> currentSection = "manifest"
                        line.lowercase().contains("related") -> currentSection = "related"
                        else -> currentSection = ""
                    }
                }
                line.startsWith("- ") -> {
                    when (currentSection) {
                        "manifest" -> manifest.add(line.substring(2).trim())
                        "related" -> {
                            // Extract UUID from markdown links like [Title](uuid)
                            val uuidPattern = Regex("\\[([^]]+)]\\(([^)]+)\\)")
                            val match = uuidPattern.find(line)
                            if (match != null) {
                                relatedMemories.add(match.groupValues[2])
                            }
                        }
                    }
                }
                currentSection == "summary" && line.isNotBlank() -> {
                    if (summaryBuilder.isNotEmpty()) summaryBuilder.append(" ")
                    summaryBuilder.append(line.trim())
                }
            }
        }
        
        if (currentSection == "summary" && summaryBuilder.isNotEmpty()) {
            summary = summaryBuilder.toString().trim()
        }
        
        return IndexEntry(summary, manifest, relatedMemories)
    }
}