package com.brainiac.core.search

import com.brainiac.core.model.LTMFile
import com.brainiac.core.llm.LLMService
import com.brainiac.core.fs.FileSystemService
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import kotlin.streams.toList

class LLMSearchService(
    private val llmService: LLMService,
    private val fileSystemService: FileSystemService,
    private val ltmRootPath: Path = Paths.get("memory", "long_term")
) : SearchService {

    override fun searchLTM(query: String): List<LTMFile> {
        if (query.isEmpty()) {
            return emptyList()
        }

        // Generate XML tree of LTM directory structure
        val xmlTree = generateLTMXmlTree()
        if (xmlTree.isEmpty()) {
            return emptyList()
        }
        
        // Ask LLM to select relevant files from the XML tree
        val prompt = buildString {
            appendLine("Given this search query: \"$query\"")
            appendLine()
            appendLine("Here is the complete structure of available long-term memory files:")
            appendLine(xmlTree)
            appendLine()
            appendLine("Please select the most relevant memory files for this query.")
            appendLine("Return only the file paths (relative to the LTM root), one per line.")
            appendLine("Do not include any other text or explanations.")
        }

        val response = llmService.generateResponse(prompt)
        
        // Parse file paths from LLM response and read the files
        return parseSelectedFiles(response)
    }

    private fun generateLTMXmlTree(): String {
        if (!Files.exists(ltmRootPath)) {
            return ""
        }

        val xmlBuilder = StringBuilder()
        xmlBuilder.appendLine("<ltm_directory>")
        
        try {
            Files.walk(ltmRootPath).use { stream ->
                val paths = stream.sorted().toList()
                buildXmlStructure(paths, ltmRootPath, xmlBuilder, 1)
            }
        } catch (e: Exception) {
            return ""
        }
        
        xmlBuilder.appendLine("</ltm_directory>")
        return xmlBuilder.toString()
    }

    private fun buildXmlStructure(paths: List<Path>, basePath: Path, xmlBuilder: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val groupedByParent = paths.groupBy { it.parent }
        
        fun processDirectory(dirPath: Path) {
            val relativeDirPath = basePath.relativize(dirPath)
            val dirName = if (relativeDirPath.toString().isEmpty()) "." else relativeDirPath.fileName.toString()
            
            if (dirPath != basePath) {
                xmlBuilder.appendLine("$indent<directory name=\"$dirName\">")
            }
            
            val children = groupedByParent[dirPath] ?: emptyList()
            val directories = children.filter { Files.isDirectory(it) }
            val files = children.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".md") && !it.fileName.toString().startsWith("_index") }
            
            // Add files first
            files.forEach { file ->
                val fileName = file.fileName.toString()
                val relativeFilePath = basePath.relativize(file)
                xmlBuilder.appendLine("$indent  <file path=\"$relativeFilePath\">$fileName</file>")
            }
            
            // Then process subdirectories recursively
            directories.forEach { subDir ->
                processDirectory(subDir)
            }
            
            if (dirPath != basePath) {
                xmlBuilder.appendLine("$indent</directory>")
            }
        }
        
        processDirectory(basePath)
    }

    private fun parseSelectedFiles(llmResponse: String): List<LTMFile> {
        val result = mutableListOf<LTMFile>()
        
        llmResponse.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("<") && !it.startsWith("#") }
            .forEach { filePath ->
                try {
                    val fullPath = ltmRootPath.resolve(filePath)
                    if (Files.exists(fullPath) && Files.isRegularFile(fullPath)) {
                        val ltmFile = fileSystemService.readLtmFile(fullPath)
                        result.add(ltmFile)
                    }
                } catch (e: Exception) {
                    // Skip files that can't be read
                }
            }
        
        return result
    }
}