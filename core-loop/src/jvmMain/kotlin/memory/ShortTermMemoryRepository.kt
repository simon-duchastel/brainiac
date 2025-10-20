package com.duchastel.simon.brainiac.core.process.memory

import kotlinx.serialization.json.Json
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
    private val json = Json { prettyPrint = true }

    /**
     * Reads the short-term memory from disk.
     *
     * @return The contents of the short-term memory file, or an empty ShortTermMemory if the file doesn't exist
     */
    fun getShortTermMemory(): ShortTermMemory {
        return if (fileSystem.exists(memoryFilePath)) {
            val rawMemory = fileSystem.read(memoryFilePath) {
                readUtf8()
            }
            json.decodeFromString<ShortTermMemory>(rawMemory)
        } else {
            ShortTermMemory()
        }
    }

    /**
     * Writes the short-term memory to disk.
     *
     * Creates the parent directory if it doesn't exist.
     *
     * @param shortTermMemory The memory content to write to disk
     */
    fun updateShortTermMemory(shortTermMemory: ShortTermMemory) {
        fileSystem.createDirectories(memoryFilePath)
        val jsonContent = json.encodeToString(shortTermMemory)
        fileSystem.write(memoryFilePath) {
            writeUtf8(jsonContent)
        }
    }

    /**
     * Updates the goals in short-term memory.
     *
     * @param updatedGoals The new list of goals to store
     */
    fun updateGoals(updatedGoals: List<Goal>) {
        val currentMemory = getShortTermMemory()
        val updatedMemory = currentMemory.copy(goals = updatedGoals)
        updateShortTermMemory(updatedMemory)
    }

    /**
     * Adds a new event to short-term memory.
     *
     * @param newEvent The event to add to the event list
     */
    fun addEvent(newEvent: String) {
        val currentMemory = getShortTermMemory()
        val updatedEvents = currentMemory.events + newEvent
        val updatedMemory = currentMemory.copy(events = updatedEvents)
        updateShortTermMemory(updatedMemory)
    }

    /**
     * Updates the thoughts in short-term memory.
     *
     * @param updatedThoughts The new list of thoughts to store
     */
    fun updateThoughts(updatedThoughts: List<String>) {
        val currentMemory = getShortTermMemory()
        val updatedMemory = currentMemory.copy(thoughts = updatedThoughts)
        updateShortTermMemory(updatedMemory)
    }

    companion object {
        /**
         * Returns the default path for the short-term memory file.
         *
         * @return Path to brainiacRootDirectory/short-term-memory.json
         */
        private fun defaultMemoryPath(brainiacRootDirectory: Path): Path {
            return brainiacRootDirectory / "short-term-memory.json"
        }
    }
}
