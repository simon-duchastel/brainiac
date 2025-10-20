package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.prompt.text.TextContentBuilderBase
import ai.koog.prompt.xml.XmlContentBuilder
import ai.koog.prompt.xml.xml
import okio.FileSystem
import okio.Path

/**
 * Repository for managing long-term memory persistence.
 *
 * Handles reading and writing long-term memory files to disk, with support
 * for multiple memory files organized in a directory structure.
 */
class LongTermMemoryRepository(
    brainiacRootDirectory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val ltmDirectory: Path = defaultLTMDirectory(brainiacRootDirectory)

    /**
     * Reads a specific long-term memory file.
     */
    fun getLongTermMemory(memoryPath: String): String {
        val fullPath = ltmDirectory / memoryPath

        return fileSystem.read(fullPath) {
            readUtf8()
        }
    }

    /**
     * Writes content to a specific long-term memory file.
     *
     * Creates parent directories if they don't exist.
     */
    fun writeLongTermMemory(memoryPath: String, content: String) {
        val filePath = ltmDirectory / memoryPath
        fileSystem.createDirectories(filePath)
        fileSystem.write(filePath) {
            writeUtf8(content)
        }
    }

    /**
     * Generates an XML mind map representing the directory structure of LTM.
     *
     * The mind map includes folder and file names but no file contents.
     */
    context(textBuilder: TextContentBuilderBase<*>)
    fun generateMindMap(indented: Boolean = true) = textBuilder.xml(indented) {
        tag("mind-map") {
            if (fileSystem.exists(ltmDirectory)) {
                generateMindMapRecursive(ltmDirectory)
            }
        }
    }

    /**
     * Recursively generates XML nodes for the directory tree.
     */
    private fun XmlContentBuilder.generateMindMapRecursive(directory: Path) {
        val entries = try {
            fileSystem.list(directory).sorted()
        } catch (e: Exception) {
            return
        }

        for (entry in entries) {
            val metadata = fileSystem.metadata(entry)
            val name = entry.name

            when {
                metadata.isDirectory -> {
                    tag("folder", linkedMapOf("name" to name)) {
                        generateMindMapRecursive(entry)
                    }
                }
                metadata.isRegularFile -> {
                    tag("file", linkedMapOf("name" to name))
                }
            }
        }
    }

    companion object {
        /**
         * Returns the default path for the long-term memory directory.
         *
         * @return Path to brainiacRootDirectory/long-term-memory/
         */
        private fun defaultLTMDirectory(brainiacRootDirectory: Path): Path {
            return brainiacRootDirectory / "long-term-memory"
        }
    }
}
