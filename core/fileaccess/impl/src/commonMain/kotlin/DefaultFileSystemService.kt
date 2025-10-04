@file:OptIn(ExperimentalTime::class)

package com.brainiac.core.fileaccess

import okio.Path
import okio.FileSystem
import com.charleskorn.kaml.Yaml
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking

/**
 * In-memory implementation of file locking for multiplatform compatibility
 */
private class InMemoryFileLock(
    private val path: Path,
    private val lockMap: MutableMap<Path, InMemoryFileLock>
) : FileLock {
    private var released = false

    override fun release() {
        if (!released) {
            released = true
            lockMap.remove(path)
        }
    }

    override fun isValid(): Boolean = !released
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

class DefaultFileSystemService(
    private val stmFilePath: Path,
    private val fileSystem: FileSystem
) : FileSystemService {
    private val yaml =
        Yaml(
            serializersModule = SerializersModule {
                contextual(InstantSerializer)
            }
        )
    private val locks = mutableMapOf<Path, InMemoryFileLock>()
    private val mutex = Mutex()

    override fun read(path: Path): String {
        return fileSystem.read(path) { readUtf8() }
    }

    override fun write(path: Path, content: String) {
        path.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(path) { writeUtf8(content) }
    }

    override fun readLtmFile(path: Path): LTMFile {
        val content = read(path)
        val parts = content.split("---\n", limit = 3)

        if (parts.size < 3) {
            throw IllegalArgumentException("Invalid LTM file format: missing frontmatter")
        }

        val frontmatter = yaml.decodeFromString(LTMFrontmatter.serializer(), parts[1])
        val markdownContent = parts[2]

        return LTMFile(frontmatter, markdownContent)
    }

    override fun writeLtmFile(path: Path, ltmFile: LTMFile) {
        val frontmatterYaml = yaml.encodeToString(LTMFrontmatter.serializer(), ltmFile.frontmatter)
        val content = "---\n$frontmatterYaml---\n${ltmFile.content}"
        write(path, content)
    }

    override fun readStm(): String {
        return try {
            if (!fileSystem.exists(stmFilePath)) {
                return ""
            }
            read(stmFilePath)
        } catch (e: Exception) {
            ""
        }
    }

    override fun writeStm(stm: ShortTermMemory) {
        try {
            val markdownContent = generateStmMarkdown(stm)
            write(stmFilePath, markdownContent)
        } catch (e: Exception) {
            throw RuntimeException("Failed to write STM file", e)
        }
    }

    private fun generateStmMarkdown(stm: ShortTermMemory): String {
        val builder = StringBuilder()

        // Header
        builder.appendLine("# Short-Term Memory")
        builder.appendLine()

        // Summary section
        builder.appendLine("## Summary")
        if (stm.summary.isNotEmpty()) {
            builder.appendLine(stm.summary)
        }
        builder.appendLine()
        builder.appendLine("---")

        // Structured Data section
        builder.appendLine("## Structured Data")
        builder.appendLine()

        // Goals subsection
        builder.appendLine("### Goals")
        if (stm.structuredData.goals.isNotEmpty()) {
            stm.structuredData.goals.forEach { goal ->
                builder.appendLine("- [ ] $goal")
            }
        }
        builder.appendLine()

        // Key Facts & Decisions subsection
        builder.appendLine("### Key Facts & Decisions")
        if (stm.structuredData.keyFacts.isNotEmpty()) {
            stm.structuredData.keyFacts.forEach { fact ->
                builder.appendLine("- $fact")
            }
        }
        builder.appendLine()

        // Tasks subsection
        builder.appendLine("### Tasks")
        if (stm.structuredData.tasks.isNotEmpty()) {
            stm.structuredData.tasks.forEach { task ->
                builder.appendLine("- [ ] $task")
            }
        }
        builder.appendLine()
        builder.appendLine("---")

        // Event Log section
        builder.appendLine("## Event Log")
        builder.appendLine("A reverse-chronological log of recent interactions. New events are appended to the top.")
        builder.appendLine()

        if (stm.eventLog.isNotEmpty()) {
            stm.eventLog.forEach { event ->
                builder.appendLine("### ${event.timestamp}")
                builder.appendLine("**User:** \"${event.user}\"")
                builder.appendLine("**AI:** \"${event.ai}\"")
                if (event.thoughts.isNotEmpty()) {
                    builder.appendLine("**Thoughts:** ${event.thoughts}")
                }
                builder.appendLine()
            }
        }

        return builder.toString()
    }

    override fun acquireLock(path: Path): FileLock = runBlocking {
        mutex.withLock {
            if (locks.containsKey(path)) {
                throw IllegalStateException("Lock already acquired for path: $path")
            }

            // Ensure parent directories exist
            path.parent?.let { fileSystem.createDirectories(it) }

            val lock = InMemoryFileLock(path, locks)
            locks[path] = lock
            lock
        }
    }

    override fun releaseLock(path: Path) {
        runBlocking {
            mutex.withLock {
                locks[path]?.release()
            }
        }
    }

    private fun logAccess(action: String, path: Path) {
        // TODO: Implement access logging for Organization process
        // val entry = AccessLogEntry(
        //     timestamp = Instant.now(),
        //     action = action,
        //     path = path.toString()
        // )
    }
}
