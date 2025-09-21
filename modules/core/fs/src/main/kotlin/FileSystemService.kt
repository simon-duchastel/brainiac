package com.braniac.core.fs

import com.braniac.core.model.*
import java.nio.file.Path
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

class FileSystemService {
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
        TODO("Not yet implemented")

        return ShortTermMemory(
            summary = "",
            structuredData = StructuredData(emptyList(), emptyList(), emptyList()),
            eventLog = emptyList()
        )
    }

    fun writeStm(stm: ShortTermMemory) {
        TODO("Not yet implemented")
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
        val entry = AccessLogEntry(
            timestamp = Instant.now(),
            action = action,
            path = path.toString()
        )

        TODO("Not yet implemented")
    }
}