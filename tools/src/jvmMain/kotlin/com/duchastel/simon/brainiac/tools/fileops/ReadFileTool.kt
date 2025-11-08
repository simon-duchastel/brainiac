package com.duchastel.simon.brainiac.tools.fileops

import ai.koog.agents.core.tools.SimpleTool
import com.duchastel.simon.brainiac.tools.BrainiacTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File

/**
 * Arguments for read file tool.
 *
 * @property path The path to the file to read
 */
@Serializable
data class ReadFileArgs(
    val path: String
)

/**
 * Tool for reading file contents.
 *
 * This tool allows AI agents to read the contents of files from the filesystem.
 */
class ReadFileTool : BrainiacTool {
    override fun toKoogTool(): SimpleTool<ReadFileArgs> {
        return object : SimpleTool<ReadFileArgs>() {
            override val name: String = "read_file"

            override val description: String = """
                Read the contents of a file from the filesystem.
                Returns the complete file contents as a string.
                Useful for examining configuration files, source code, logs, etc.
            """.trimIndent()

            override val argsSerializer: KSerializer<ReadFileArgs> = serializer()

            override suspend fun doExecute(args: ReadFileArgs): String {
                return try {
                    val file = File(args.path)
                    if (!file.exists()) {
                        return """{"error": "File not found: ${args.path}"}"""
                    }
                    if (!file.isFile) {
                        return """{"error": "Path is not a file: ${args.path}"}"""
                    }
                    file.readText()
                } catch (e: Exception) {
                    """{"error": "${e.message}"}"""
                }
            }
        }
    }
}
