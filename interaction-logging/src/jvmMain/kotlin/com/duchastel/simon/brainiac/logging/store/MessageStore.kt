package com.duchastel.simon.brainiac.logging.store

import com.duchastel.simon.brainiac.logging.config.InteractionLoggingConfig
import com.duchastel.simon.brainiac.logging.models.LoggedMessage
import com.duchastel.simon.brainiac.logging.models.MessageMetadata
import com.duchastel.simon.brainiac.logging.models.MessageRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import java.security.MessageDigest
import java.time.Instant

/**
 * Store for deduplicated messages.
 * Messages are identified by SHA-256 hash of their content.
 * Same content = same ID, stored only once.
 */
class MessageStore(
    private val config: InteractionLoggingConfig,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    init {
        ensureDirectoriesExist()
    }

    private fun ensureDirectoriesExist() {
        if (!fileSystem.exists(config.messagesDirectory)) {
            fileSystem.createDirectories(config.messagesDirectory)
        }
    }

    /**
     * Compute SHA-256 hash of content to use as message ID.
     */
    fun computeMessageId(content: String, role: MessageRole): String {
        val input = "${role.name}:$content"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get the file path for a message ID.
     * If sharding is enabled, uses first 2 chars as subdirectory.
     */
    private fun getMessagePath(messageId: String): Path {
        return if (config.shardMessages) {
            val shard = messageId.take(2)
            val shardDir = config.messagesDirectory / shard
            if (!fileSystem.exists(shardDir)) {
                fileSystem.createDirectories(shardDir)
            }
            shardDir / "$messageId.json"
        } else {
            config.messagesDirectory / "$messageId.json"
        }
    }

    /**
     * Store a message. Returns the message ID.
     * If message already exists (same hash), returns existing ID without overwriting.
     */
    fun storeMessage(
        content: String,
        role: MessageRole,
        metadata: MessageMetadata? = null
    ): String {
        val messageId = computeMessageId(content, role)
        val messagePath = getMessagePath(messageId)

        // Deduplication: if file exists, message is already stored
        if (fileSystem.exists(messagePath)) {
            return messageId
        }

        val message = LoggedMessage(
            id = messageId,
            timestamp = Instant.now().toString(),
            role = role,
            content = content,
            metadata = metadata
        )

        fileSystem.write(messagePath) {
            writeUtf8(json.encodeToString(message))
        }

        return messageId
    }

    /**
     * Retrieve a message by ID.
     */
    fun getMessage(messageId: String): LoggedMessage? {
        val messagePath = getMessagePath(messageId)

        if (!fileSystem.exists(messagePath)) {
            return null
        }

        return try {
            val content = fileSystem.read(messagePath) { readUtf8() }
            json.decodeFromString<LoggedMessage>(content)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a message exists.
     */
    fun messageExists(messageId: String): Boolean {
        return fileSystem.exists(getMessagePath(messageId))
    }

    /**
     * List all message IDs in the store.
     */
    fun listMessageIds(): List<String> {
        if (!fileSystem.exists(config.messagesDirectory)) {
            return emptyList()
        }

        return if (config.shardMessages) {
            fileSystem.list(config.messagesDirectory)
                .filter { fileSystem.metadata(it).isDirectory }
                .flatMap { shardDir -> fileSystem.list(shardDir) }
                .filter { it.name.endsWith(".json") }
                .map { it.name.removeSuffix(".json") }
        } else {
            fileSystem.list(config.messagesDirectory)
                .filter { it.name.endsWith(".json") }
                .map { it.name.removeSuffix(".json") }
        }
    }
}
