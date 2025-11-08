package com.duchastel.simon.brainiac.tools.bash

import ai.koog.agents.core.tools.SimpleTool
import com.duchastel.simon.brainiac.tools.BrainiacTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Arguments for bash command execution tool.
 *
 * @property command The bash command to execute
 * @property timeoutSeconds Timeout for command execution in seconds (default: 30)
 */
@Serializable
data class BashArgs(
    val command: String,
    val timeoutSeconds: Int = 30
)

/**
 * Tool for executing bash commands.
 *
 * This tool allows AI agents to execute bash commands on the system.
 * Commands are executed with a configurable timeout for safety.
 */
class BashTool : BrainiacTool {
    override fun toKoogTool(): SimpleTool<BashArgs> {
        return object : SimpleTool<BashArgs>() {
            override val name: String = "bash_execute"

            override val description: String = """
                Execute a bash command on the system.
                This tool runs commands in a bash shell and returns the output (stdout and stderr).
                Commands have a configurable timeout for safety.
                Use this for system operations, file management, running scripts, etc.
            """.trimIndent()

            override val argsSerializer: KSerializer<BashArgs> = serializer()

            override suspend fun doExecute(args: BashArgs): String {
                return try {
                    val processBuilder = ProcessBuilder("bash", "-c", args.command)
                    processBuilder.redirectErrorStream(true)

                    val process = processBuilder.start()
                    val output = StringBuilder()

                    // Read output
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            output.append(line).append("\n")
                        }
                    }

                    // Wait for process with timeout
                    val completed = process.waitFor(args.timeoutSeconds.toLong(), TimeUnit.SECONDS)

                    if (!completed) {
                        process.destroyForcibly()
                        return """{"error": "Command timed out after ${args.timeoutSeconds} seconds"}"""
                    }

                    val exitCode = process.exitValue()

                    if (exitCode == 0) {
                        output.toString()
                    } else {
                        """{"error": "Command exited with code $exitCode", "output": "${output.toString().replace("\"", "\\\"")}"}"""
                    }
                } catch (e: Exception) {
                    """{"error": "${e.message}"}"""
                }
            }
        }
    }
}
