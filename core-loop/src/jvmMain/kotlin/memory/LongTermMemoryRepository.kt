package com.duchastel.simon.brainiac.core.process.memory

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

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
     * Lists all long-term memory file paths in the LTM directory.
     *
     * @return List of relative file paths within the LTM directory
     */
    fun listFiles(): List<String> {
        if (!fileSystem.exists(ltmDirectory)) {
            return emptyList()
        }

        val files = mutableListOf<String>()
        collectFiles(ltmDirectory, "", files)
        return files
    }

    /**
     * Recursively collects file paths relative to the LTM directory.
     */
    private fun collectFiles(directory: Path, relativePath: String, files: MutableList<String>) {
        val entries = fileSystem.list(directory)

        for (entry in entries) {
            val metadata = fileSystem.metadata(entry)
            val entryName = entry.name
            val newRelativePath = if (relativePath.isEmpty()) entryName else "$relativePath/$entryName"

            when {
                metadata.isDirectory -> collectFiles(entry, newRelativePath, files)
                metadata.isRegularFile -> files.add(newRelativePath)
            }
        }
    }

    /**
     * Reads a specific long-term memory file.
     *
     * @param relativePath The relative path to the file within the LTM directory
     * @return The contents of the file, or an empty string if the file doesn't exist
     */
    fun read(relativePath: String): String {
        val filePath = ltmDirectory / relativePath
        return if (fileSystem.exists(filePath)) {
            fileSystem.read(filePath) {
                readUtf8()
            }
        } else {
            ""
        }
    }

    /**
     * Writes content to a specific long-term memory file.
     *
     * Creates parent directories if they don't exist.
     *
     * @param relativePath The relative path to the file within the LTM directory
     * @param content The content to write to the file
     */
    fun write(relativePath: String, content: String) {
        val filePath = ltmDirectory / relativePath
        filePath.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(filePath) {
            writeUtf8(content)
        }
    }

    /**
     * Generates an XML mind map representing the directory structure of LTM.
     *
     * The mind map includes folder and file names but no file contents.
     *
     * @return XML string representing the directory tree structure
     */
    fun generateMindMap(): String {
        if (!fileSystem.exists(ltmDirectory)) {
            return "<ltm-directory />"
        }

        val xml = StringBuilder()
        xml.append("<ltm-directory>\n")
        generateMindMapRecursive(ltmDirectory, 1, xml)
        xml.append("</ltm-directory>")
        return xml.toString()
    }

    /**
     * Recursively generates XML nodes for the directory tree.
     */
    private fun generateMindMapRecursive(directory: Path, depth: Int, xml: StringBuilder) {
        val entries = try {
            fileSystem.list(directory).sorted()
        } catch (e: Exception) {
            return
        }

        val indent = "  ".repeat(depth)

        for (entry in entries) {
            val metadata = fileSystem.metadata(entry)
            val name = entry.name

            when {
                metadata.isDirectory -> {
                    xml.append("$indent<folder name=\"$name\">\n")
                    generateMindMapRecursive(entry, depth + 1, xml)
                    xml.append("$indent</folder>\n")
                }
                metadata.isRegularFile -> {
                    xml.append("$indent<file name=\"$name\" />\n")
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
