package com.duchastel.simon.brainiac.tools.fileops

import ai.koog.agents.core.tools.SimpleTool
import com.duchastel.simon.brainiac.tools.BrainiacTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import java.io.File

/**
 * Arguments for list directory tool.
 *
 * @property path The path to the directory to list
 * @property recursive Whether to list files recursively (default: false)
 */
@Serializable
data class ListDirectoryArgs(
    val path: String,
    val recursive: Boolean = false
)

/**
 * Tool for listing directory contents.
 *
 * This tool allows AI agents to list the contents of directories.
 * Supports recursive listing and returns file/directory information.
 */
class ListDirectoryTool : BrainiacTool {
    override fun toKoogTool(): SimpleTool<ListDirectoryArgs> {
        return object : SimpleTool<ListDirectoryArgs>() {
            override val name: String = "list_directory"

            override val description: String = """
                List the contents of a directory.
                Returns information about files and subdirectories including names, types, and sizes.
                Can optionally list recursively to show all nested files.
            """.trimIndent()

            override val argsSerializer: KSerializer<ListDirectoryArgs> = serializer()

            override suspend fun doExecute(args: ListDirectoryArgs): String {
                return try {
                    val dir = File(args.path)
                    if (!dir.exists()) {
                        return """{"error": "Directory not found: ${args.path}"}"""
                    }
                    if (!dir.isDirectory) {
                        return """{"error": "Path is not a directory: ${args.path}"}"""
                    }

                    val entries = if (args.recursive) {
                        dir.walkTopDown().filter { it != dir }.toList()
                    } else {
                        dir.listFiles()?.toList() ?: emptyList()
                    }

                    buildJsonObject {
                        put("path", args.path)
                        put("recursive", args.recursive)
                        putJsonArray("entries") {
                            entries.forEach { file ->
                                addJsonObject {
                                    put("name", file.name)
                                    put("path", file.path)
                                    put("isDirectory", file.isDirectory)
                                    put("isFile", file.isFile)
                                    if (file.isFile) {
                                        put("size", file.length())
                                    }
                                }
                            }
                        }
                    }.toString()
                } catch (e: Exception) {
                    """{"error": "${e.message}"}"""
                }
            }
        }
    }
}
