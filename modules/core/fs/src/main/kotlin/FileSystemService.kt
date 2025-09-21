package com.brainiac.core.fs

import com.brainiac.core.model.*
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.time.Instant
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

class FileSystemService(
    private val stmFilePath: Path = Paths.get("memory", "short_term.md")
) {
    private val yaml = Yaml(
        serializersModule = SerializersModule {
            contextual(InstantSerializer)
        }
    )
    private val locks = mutableMapOf<Path, FileChannel>()

    fun read(path: Path): String {
        return Files.readString(path)
    }

    fun write(path: Path, content: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }

    fun readLtmFile(path: Path): LTMFile {
        val content = read(path)
        val parts = content.split("---\n", limit = 3)
        
        if (parts.size < 3) {
            throw IllegalArgumentException("Invalid LTM file format: missing frontmatter")
        }
        
        val frontmatter = yaml.decodeFromString(LTMFrontmatter.serializer(), parts[1])
        val markdownContent = parts[2]
        
        return LTMFile(frontmatter, markdownContent)
    }

    fun writeLtmFile(path: Path, ltmFile: LTMFile) {
        val frontmatterYaml = yaml.encodeToString(LTMFrontmatter.serializer(), ltmFile.frontmatter)
        val content = "---\n$frontmatterYaml---\n${ltmFile.content}"
        write(path, content)
    }

    fun readStm(): ShortTermMemory {
        return try {
            if (!Files.exists(stmFilePath)) {
                return createEmptyStm()
            }
            val content = read(stmFilePath)
            parseStmMarkdown(content)
        } catch (e: Exception) {
            createEmptyStm()
        }
    }
    
    private fun createEmptyStm(): ShortTermMemory {
        return ShortTermMemory(
            summary = "",
            structuredData = StructuredData(emptyList(), emptyList(), emptyList()),
            eventLog = emptyList()
        )
    }
    
    private fun parseStmMarkdown(content: String): ShortTermMemory {
        val lines = content.lines()
        var currentSection = ""
        var summary = ""
        val goals = mutableListOf<String>()
        val keyFacts = mutableListOf<String>()
        val tasks = mutableListOf<String>()
        val events = mutableListOf<Event>()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            when {
                line.startsWith("## Summary") -> {
                    currentSection = "summary"
                    i++
                    // Read summary content until next section or ---
                    val summaryLines = mutableListOf<String>()
                    while (i < lines.size) {
                        val summaryLine = lines[i]
                        if (summaryLine.trim() == "---" || summaryLine.trim().startsWith("##")) {
                            break
                        }
                        if (summaryLine.trim().isNotEmpty()) {
                            summaryLines.add(summaryLine)
                        }
                        i++
                    }
                    summary = summaryLines.joinToString("\n").trim()
                    continue
                }
                
                line.startsWith("## Structured Data") -> {
                    currentSection = "structured"
                }
                
                line.startsWith("### Goals") -> {
                    currentSection = "goals"
                }
                
                line.startsWith("### Key Facts") -> {
                    currentSection = "facts"
                }
                
                line.startsWith("### Tasks") -> {
                    currentSection = "tasks"
                }
                
                line.startsWith("## Event Log") -> {
                    currentSection = "events"
                }
                
                line.startsWith("### ") && currentSection == "events" -> {
                    // Parse event timestamp
                    val timestampStr = line.substring(4).trim()
                    try {
                        val timestamp = Instant.parse(timestampStr)
                        val event = parseEventEntry(lines, i + 1, timestamp)
                        if (event != null) {
                            events.add(event)
                        }
                    } catch (e: Exception) {
                        // Skip malformed timestamp
                    }
                }
                
                line.startsWith("- [ ]") -> {
                    val item = line.substring(5).trim()
                    when (currentSection) {
                        "goals" -> goals.add(item)
                        "tasks" -> tasks.add(item)
                    }
                }
                
                line.startsWith("- ") -> {
                    val item = line.substring(2).trim()
                    when (currentSection) {
                        "facts" -> keyFacts.add(item)
                        "goals" -> goals.add(item)
                        "tasks" -> tasks.add(item)
                    }
                }
            }
            i++
        }
        
        return ShortTermMemory(
            summary = summary,
            structuredData = StructuredData(goals, keyFacts, tasks),
            eventLog = events
        )
    }
    
    private fun parseEventEntry(lines: List<String>, startIndex: Int, timestamp: Instant): Event? {
        var user = ""
        var ai = ""
        var thoughts = ""
        
        var i = startIndex
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.startsWith("### ") || line.startsWith("## ")) {
                // Next section reached
                break
            }
            
            when {
                line.startsWith("**User:**") -> {
                    user = extractQuotedText(line.substring(9).trim())
                }
                line.startsWith("**AI:**") -> {
                    ai = extractQuotedText(line.substring(7).trim())
                }
                line.startsWith("**Thoughts:**") -> {
                    thoughts = line.substring(13).trim()
                }
            }
            i++
        }
        
        return if (user.isNotEmpty() && ai.isNotEmpty()) {
            Event(timestamp, user, ai, thoughts)
        } else {
            null
        }
    }
    
    private fun extractQuotedText(text: String): String {
        return if (text.startsWith("\"") && text.endsWith("\"") && text.length >= 2) {
            text.substring(1, text.length - 1)
        } else {
            text
        }
    }

    fun writeStm(stm: ShortTermMemory) {
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
        builder.appendLine("# Short-Term Memory Scratchpad")
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
        builder.appendLine("This section contains discrete, machine-readable data for immediate use.")
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

    @Synchronized
    fun acquireLock(path: Path): FileLock {
        if (locks.containsKey(path)) {
            throw IllegalStateException("Lock already acquired for path: $path")
        }
        
        Files.createDirectories(path.parent)
        val channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        val lock = channel.lock()
        locks[path] = channel
        return lock
    }

    @Synchronized
    fun releaseLock(path: Path) {
        locks[path]?.let { channel ->
            channel.close()
            locks.remove(path)
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