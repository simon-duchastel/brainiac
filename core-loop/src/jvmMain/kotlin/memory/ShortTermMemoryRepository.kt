package com.duchastel.simon.brainiac.core.process.memory

import okio.FileSystem
import okio.Path

/**
 * Repository for managing short-term memory persistence.
 *
 * Handles reading and writing short-term memory to disk, with a default
 * location in the user's home directory.
 */
class ShortTermMemoryRepository(
    brainiacRootDirectory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val memoryFilePath: Path = defaultMemoryPath(brainiacRootDirectory)

    /**
     * Reads the short-term memory from disk.
     *
     * @return The contents of the short-term memory file, or an empty string if the file doesn't exist
     */
    fun read(): ShortTermMemory {
        val rawMemory = if (fileSystem.exists(memoryFilePath)) {
            fileSystem.read(memoryFilePath) {
                readUtf8()
            }
        } else {
            ""
        }
        return ShortTermMemory(rawMemory)
    }

    /**
     * Writes the short-term memory to disk.
     *
     * Creates the parent directory if it doesn't exist.
     *
     * @param memory The memory content to write to disk
     */
    fun write(memory: String) {
        memoryFilePath.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(memoryFilePath) {
            writeUtf8(memory)
        }
    }

    companion object {
        /**
         * Returns the default path for the short-term memory file.
         *
         * @return Path to brainiacRootDirectory/short-term-memory.txt
         */
        private fun defaultMemoryPath(brainiacRootDirectory: Path): Path {
            return brainiacRootDirectory / "short-term-memory.txt"
        }
    }
}
