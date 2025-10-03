package com.brainiac.core.search

import com.brainiac.core.fileaccess.LTMFile
import com.brainiac.core.llm.LLMService
import com.brainiac.core.fileaccess.FileSystemService
import okio.Path
import okio.Path.Companion.toPath
import okio.FileSystem

class LLMSearchService(
    private val llmService: LLMService,
    private val fileSystemService: FileSystemService,
    private val ltmRootPath: Path,
    private val fileSystem: FileSystem
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
        if (!fileSystem.exists(ltmRootPath)) {
            return ""
        }

        val xmlBuilder = StringBuilder()
        xmlBuilder.appendLine("<ltm_directory>")
        
        try {
            val allPaths = collectAllPaths(ltmRootPath)
            buildXmlStructure(allPaths, ltmRootPath, xmlBuilder, 1)
        } catch (e: Exception) {
            return ""
        }
        
        xmlBuilder.appendLine("</ltm_directory>")
        return xmlBuilder.toString()
    }

    private fun collectAllPaths(rootPath: Path): List<Path> {
        val result = mutableListOf<Path>()
        
        fun collectRecursively(path: Path) {
            result.add(path)
            try {
                if (fileSystem.metadata(path).isDirectory) {
                    fileSystem.list(path).sorted().forEach { childPath ->
                        collectRecursively(childPath)
                    }
                }
            } catch (e: Exception) {
                // Skip directories that can't be read
            }
        }
        
        collectRecursively(rootPath)
        return result
    }

    private fun buildXmlStructure(paths: List<Path>, basePath: Path, xmlBuilder: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val groupedByParent = paths.groupBy { it.parent }
        
        fun processDirectory(dirPath: Path) {
            val relativeDirPath = relativizePath(basePath, dirPath)
            val dirName = if (relativeDirPath.isEmpty()) "." else dirPath.name
            
            if (dirPath != basePath) {
                xmlBuilder.appendLine("$indent<directory name=\"$dirName\">")
            }
            
            val children = groupedByParent[dirPath] ?: emptyList()
            val directories = children.filter { 
                try { fileSystem.metadata(it).isDirectory } catch (e: Exception) { false }
            }
            val files = children.filter { 
                try {
                    fileSystem.metadata(it).isRegularFile && 
                    it.name.endsWith(".md") && 
                    !it.name.startsWith("_index")
                } catch (e: Exception) { false }
            }
            
            // Add files first
            files.forEach { file ->
                val fileName = file.name
                val relativeFilePath = relativizePath(basePath, file)
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

    private fun relativizePath(basePath: Path, targetPath: Path): String {
        val baseSegments = basePath.segments
        val targetSegments = targetPath.segments
        
        if (targetSegments.size <= baseSegments.size) {
            return ""
        }
        
        return targetSegments.drop(baseSegments.size).joinToString("/")
    }

    private fun parseSelectedFiles(llmResponse: String): List<LTMFile> {
        val result = mutableListOf<LTMFile>()
        
        llmResponse.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("<") && !it.startsWith("#") }
            .forEach { filePath ->
                try {
                    val fullPath = ltmRootPath / filePath
                    if (fileSystem.exists(fullPath) && fileSystem.metadata(fullPath).isRegularFile) {
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