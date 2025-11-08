package com.duchastel.simon.brainiac.tools.fileops

import ai.koog.agents.core.tools.SimpleTool
import com.duchastel.simon.brainiac.tools.BrainiacTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File

/**
 * Arguments for edit file tool.
 *
 * @property path The path to the file to edit
 * @property content The new content to write to the file
 */
@Serializable
data class EditFileArgs(
    val path: String,
    val content: String
)

/**
 * Tool for editing/writing file contents.
 *
 * This tool allows AI agents to write or overwrite file contents.
 * If the file doesn't exist, it will be created.
 */
class EditFileTool : BrainiacTool {
    override fun toKoogTool(): SimpleTool<EditFileArgs> {
        return object : SimpleTool<EditFileArgs>() {
            override val name: String = "edit_file"

            override val description: String = """
                Write or edit a file's contents.
                If the file doesn't exist, it will be created (including parent directories).
                If it exists, it will be overwritten with the new content.
                Use this for creating or modifying configuration files, source code, etc.
            """.trimIndent()

            override val argsSerializer: KSerializer<EditFileArgs> = serializer()

            override suspend fun doExecute(args: EditFileArgs): String {
                return try {
                    val file = File(args.path)

                    // Create parent directories if they don't exist
                    file.parentFile?.mkdirs()

                    file.writeText(args.content)
                    """{"success": true, "path": "${args.path}", "message": "File written successfully"}"""
                } catch (e: Exception) {
                    """{"error": "${e.message}"}"""
                }
            }
        }
    }
}
