package com.braniac.core.fs

import com.braniac.core.model.*
import java.nio.file.Path
import java.nio.file.Files
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.time.Instant
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

class FileSystemService {
    private val yamlMapper = YAMLMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
        
        val frontmatter = yamlMapper.readValue<LTMFrontmatter>(parts[1])
        val markdownContent = parts[2]
        
        return LTMFile(frontmatter, markdownContent)
    }

    fun writeLtmFile(path: Path, ltmFile: LTMFile) {
        val frontmatterYaml = yamlMapper.writeValueAsString(ltmFile.frontmatter)
        val content = "---\n$frontmatterYaml---\n${ltmFile.content}"
        write(path, content)
    }

    fun readStm(): ShortTermMemory {
        // Placeholder - would read from configured STM path
        return ShortTermMemory(
            summary = "",
            structuredData = StructuredData(emptyList(), emptyList(), emptyList()),
            eventLog = emptyList()
        )
    }

    fun writeStm(stm: ShortTermMemory) {
        // Placeholder - would write to configured STM path
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

    fun logAccess(action: String, path: Path) {
        val entry = AccessLogEntry(
            timestamp = Instant.now(),
            action = action,
            path = path.toString()
        )
        // Placeholder - would append to access.log file
    }
}