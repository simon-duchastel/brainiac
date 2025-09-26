package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.fs.FileSystemService
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.UUID
import kotlin.streams.toList

class DefaultSearchService(
    private val fileSystemService: FileSystemService,
    private val ltmRootPath: Path = Paths.get("memory", "long_term")
) : SearchService {

    data class SearchResult(
        val ltmFile: LTMFile,
        val path: Path,
        val relevanceScore: Double
    )

    data class IndexEntry(
        val summary: String,
        val manifest: List<String>,
        val relatedMemories: List<String>
    )

    override fun searchLTM(queries: List<String>): List<LTMFile> {
        if (queries.isEmpty() || !Files.exists(ltmRootPath)) {
            return emptyList()
        }

        val ltmFiles = findAllLtmFiles()
        val indexEntries = findAllIndexEntries()
        
        val searchResults = ltmFiles.mapNotNull { (file, path) ->
            val score = calculateRelevanceScore(file, path, queries, indexEntries)
            if (score > 0.0) {
                SearchResult(file, path, score)
            } else {
                null
            }
        }

        return searchResults
            .sortedByDescending { it.relevanceScore }
            .map { it.ltmFile }
    }

    private fun findAllLtmFiles(): List<Pair<LTMFile, Path>> {
        if (!Files.exists(ltmRootPath)) {
            return emptyList()
        }

        return Files.walk(ltmRootPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(".md") }
                .filter { !it.fileName.toString().startsWith("_index") }
                .toList()
                .mapNotNull { path ->
                    try {
                        val ltmFile = fileSystemService.readLtmFile(path)
                        Pair(ltmFile, path)
                    } catch (e: Exception) {
                        // Skip files that can't be parsed as LTM files
                        null
                    }
                }
        }
    }

    private fun findAllIndexEntries(): Map<Path, IndexEntry> {
        if (!Files.exists(ltmRootPath)) {
            return emptyMap()
        }

        return Files.walk(ltmRootPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString() == "_index.md" }
                .toList()
                .mapNotNull { path ->
                    try {
                        val content = fileSystemService.read(path)
                        val indexEntry = parseIndexFile(content)
                        Pair(path.parent, indexEntry)
                    } catch (e: Exception) {
                        // Skip index files that can't be parsed
                        null
                    }
                }
                .toMap()
        }
    }

    private fun parseIndexFile(content: String): IndexEntry {
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

    private fun calculateRelevanceScore(
        ltmFile: LTMFile, 
        filePath: Path, 
        queries: List<String>, 
        indexEntries: Map<Path, IndexEntry>
    ): Double {
        var score = 0.0
        val normalizedQueries = queries.map { it.lowercase().trim() }

        // Content matching (weight: 1.0)
        val contentLower = ltmFile.content.lowercase()
        for (query in normalizedQueries) {
            if (contentLower.contains(query)) {
                score += 1.0
                // Bonus for exact phrase matches
                if (contentLower.contains(" $query ") || contentLower.startsWith("$query ") || contentLower.endsWith(" $query")) {
                    score += 0.5
                }
            }
        }

        // Tag matching (weight: 2.0 - higher priority)
        val tagsLower = ltmFile.frontmatter.tags.map { it.lowercase() }
        for (query in normalizedQueries) {
            for (tag in tagsLower) {
                if (tag.contains(query)) {
                    score += 2.0
                    // Exact tag match gets bonus
                    if (tag == query) {
                        score += 1.0
                    }
                }
            }
        }

        // Index-based scoring - check parent directory's _index.md
        val parentDir = filePath.parent
        indexEntries[parentDir]?.let { indexEntry ->
            // Summary matching (weight: 1.5)
            val summaryLower = indexEntry.summary.lowercase()
            for (query in normalizedQueries) {
                if (summaryLower.contains(query)) {
                    score += 1.5
                }
            }
            
            // Related memories bonus (weight: 0.5)
            // If this memory is related to others via UUID, it gets a relevance boost
            if (indexEntry.relatedMemories.contains(ltmFile.frontmatter.uuid)) {
                score += 0.5
            }
        }

        // Reinforcement count bonus (weight: 0.1 per count)
        // More reinforced memories are considered more relevant
        score += ltmFile.frontmatter.reinforcementCount * 0.1

        // Recency bonus (weight: up to 0.5)
        // More recent memories get a small boost
        val daysSinceUpdate = java.time.Duration.between(
            ltmFile.frontmatter.updatedAt,
            java.time.Instant.now()
        ).toDays()
        if (daysSinceUpdate < 30) {
            score += 0.5 * (1.0 - daysSinceUpdate / 30.0)
        }

        return score
    }
}