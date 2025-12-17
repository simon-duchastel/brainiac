# Interaction Logging Instrumentation Plan

## Overview

Add modular instrumentation to log AI interactions locally with **message deduplication** and **thread tracking**. The goal is to enable "time travel" through AI interactions while minimizing storage overhead from repeated messages in LLM conversations.

---

## Core Concepts

### Message vs Thread Model

| Concept | Definition | Storage |
|---------|-----------|---------|
| **Message** | A unique piece of content (user input, assistant response, tool call, etc.) | `messages/` directory, one file per message hash |
| **Thread** | An ordered sequence of message references with positional metadata | `threads/` directory, one JSONL file per session |
| **ThreadEntry** | A reference to a message at a specific position in context | Line in thread JSONL file |
| **MemoryOperation** | A read/write/query operation on memory files (STM, LTM) | Logged as special thread entries |

### Why This Model?

LLM conversations replay messages constantly:
```
Turn 1: [System, User1]
Turn 2: [System, User1, Assistant1, User2]
Turn 3: [System, User1, Assistant1, User2, Assistant2, User3]
...
```

Without deduplication, we'd store `System` N times, `User1` N-1 times, etc.

With this model:
- Each unique message stored **once**
- Threads reference messages by ID
- Position changes tracked per-thread-entry

---

## Architecture

```
logs/
├── interactions/
│   ├── messages/
│   │   ├── abc123.json      # Individual message (hash-named)
│   │   ├── def456.json
│   │   └── ...
│   ├── threads/
│   │   ├── session-2024-01-15T10-30-00.jsonl   # Thread file
│   │   └── session-2024-01-15T14-20-00.jsonl
│   └── index.json           # Optional: quick lookup index
```

---

## Data Structures

### Message (stored once per unique content)

```kotlin
@Serializable
data class LoggedMessage(
    val id: String,                    // SHA-256 hash of content
    val timestamp: Instant,            // First occurrence
    val role: MessageRole,             // SYSTEM, USER, ASSISTANT, TOOL_CALL, TOOL_RESULT
    val content: String,               // The actual content
    val metadata: MessageMetadata?     // Optional extras
)

@Serializable
data class MessageMetadata(
    val model: String? = null,         // LLM model used (for assistant messages)
    val toolName: String? = null,      // Tool name (for tool calls/results)
    val tokenCount: Int? = null,       // Estimated tokens
    val thinkingContent: String? = null // Extended thinking (if separate)
)

enum class MessageRole {
    SYSTEM, USER, ASSISTANT, TOOL_CALL, TOOL_RESULT
}
```

### ThreadEntry (one per context position)

```kotlin
@Serializable
data class ThreadEntry(
    val timestamp: Instant,            // When this entry was added to context
    val messageId: String?,            // Reference to LoggedMessage.id (null for memory ops)
    val positionInContext: Int?,       // 0-indexed position in LLM context (null for memory ops)
    val eventType: ThreadEventType,    // What happened
    val contextSnapshot: ContextSnapshot?, // Optional context metadata
    val memoryOperation: MemoryOperation?  // Present for MEMORY_* event types
)

enum class ThreadEventType {
    MESSAGE_ADDED,      // New message added to context
    CONTEXT_REPLAYED,   // Existing message replayed in new request
    CONTEXT_COMPACTED,  // Context was summarized/compacted
    LLM_REQUEST,        // An LLM request was made
    LLM_RESPONSE,       // LLM responded
    MEMORY_READ,        // Memory file was read (STM/LTM recall)
    MEMORY_WRITE,       // Memory file was written (STM/LTM update)
    MEMORY_QUERY        // Semantic search/query on memory
}

@Serializable
data class ContextSnapshot(
    val totalMessages: Int,
    val estimatedTokens: Int?,
    val modelUsed: String?
)

@Serializable
data class MemoryOperation(
    val memoryType: MemoryType,        // STM, LTM
    val operation: MemoryOpType,       // READ, WRITE, QUERY
    val filePath: String?,             // File accessed (for LTM)
    val query: String?,                // Search query (for QUERY ops)
    val resultCount: Int?,             // Number of results returned
    val contentPreview: String?        // First ~100 chars of content
)

enum class MemoryType { STM, LTM }
enum class MemoryOpType { READ, WRITE, QUERY }
```

### Thread File Format (JSONL)

Each line is a `ThreadEntry` JSON object:
```jsonl
{"timestamp":"2024-01-15T10:30:00Z","messageId":"abc123","positionInContext":0,"eventType":"MESSAGE_ADDED"}
{"timestamp":"2024-01-15T10:30:00Z","messageId":"def456","positionInContext":1,"eventType":"MESSAGE_ADDED"}
{"timestamp":"2024-01-15T10:30:01Z","messageId":"abc123","positionInContext":0,"eventType":"CONTEXT_REPLAYED"}
{"timestamp":"2024-01-15T10:30:01Z","messageId":"def456","positionInContext":1,"eventType":"CONTEXT_REPLAYED"}
{"timestamp":"2024-01-15T10:30:01Z","messageId":"ghi789","positionInContext":2,"eventType":"MESSAGE_ADDED"}
```

---

## Module Design

### New Module: `interaction-logging`

```
interaction-logging/
├── src/jvmMain/kotlin/
│   ├── InteractionLogger.kt           # Main interface
│   ├── InteractionLoggerImpl.kt       # Default implementation
│   ├── MessageStore.kt                # Message deduplication/storage
│   ├── ThreadStore.kt                 # Thread tracking
│   ├── models/                        # Data classes above
│   └── callbacks/                     # Koog integration
│       └── LoggingEventHandler.kt     # EventHandler feature
```

### Core Interface

```kotlin
interface InteractionLogger {
    // Message operations
    suspend fun logMessage(message: LoggedMessage): String  // Returns message ID
    suspend fun getMessage(id: String): LoggedMessage?

    // Thread operations
    suspend fun startThread(sessionId: String? = null): String  // Returns thread ID
    suspend fun logThreadEntry(threadId: String, entry: ThreadEntry)
    suspend fun getThread(threadId: String): List<ThreadEntry>

    // Convenience
    suspend fun logContextState(
        threadId: String,
        messages: List<Message>,  // Koog Message type
        eventType: ThreadEventType
    )

    // Memory operations
    suspend fun logMemoryOperation(
        threadId: String,
        memoryType: MemoryType,
        operation: MemoryOpType,
        filePath: String? = null,
        query: String? = null,
        resultCount: Int? = null,
        contentPreview: String? = null
    )

    // Query
    suspend fun listThreads(limit: Int = 100): List<ThreadSummary>
    suspend fun searchMessages(query: String): List<LoggedMessage>
}
```

### Callback Integration Point

```kotlin
// In CoreAgent.kt, add alongside existing EventHandler
install(InteractionLogging) {
    logger = interactionLogger  // Injected

    onContextAssembled { ctx ->
        // Log all messages in context with positions
    }

    onLLMRequest { ctx ->
        // Log the request event
    }

    onLLMResponse { ctx ->
        // Log response, extract and store new messages
    }
}
```

---

## Implementation Steps

### Phase 1: Core Data Layer
1. Create `interaction-logging` module with gradle setup
2. Implement `LoggedMessage`, `ThreadEntry` data classes
3. Implement `MessageStore` with hash-based deduplication
4. Implement `ThreadStore` with JSONL append-only writes

### Phase 2: Logger Implementation
5. Implement `InteractionLoggerImpl` combining stores
6. Add configuration (log directory, retention policy)
7. Add thread lifecycle management (start/end)

### Phase 3: Koog Integration
8. Create `LoggingEventHandler` Koog feature
9. Integrate into `CoreAgent.kt` via config
10. Wire up context assembly, request, response hooks

### Phase 4: CLI Integration
11. Add `--interaction-logs` CLI flag
12. Add log directory configuration
13. Optional: Add `/replay` command to view past sessions

---

## Tradeoffs Analysis

### Decision 1: Hash-Based Message IDs

**Option A: Content Hash (SHA-256)** ✓ RECOMMENDED
- Pros:
  - Automatic deduplication (same content = same ID)
  - Deterministic (reproducible)
  - No coordination needed
- Cons:
  - Hash collisions (extremely rare with SHA-256)
  - Identical content from different contexts treated as same message
  - Can't distinguish "same text, different meaning"

**Option B: UUID per Message**
- Pros:
  - Unique per occurrence
  - Can track same content appearing multiple times
- Cons:
  - No automatic deduplication
  - Storage grows linearly with replays
  - Defeats the purpose of the design

**Option C: Hybrid (Hash + Context Hash)**
- Pros:
  - Deduplication with context awareness
- Cons:
  - Complex
  - Still grows with context changes

**Decision: Option A** - Content hash provides the deduplication we need. If same text appears in different contexts, it's likely the same logical message anyway.

---

### Decision 2: Storage Format for Messages

**Option A: Individual JSON Files** ✓ RECOMMENDED
- Pros:
  - Simple to implement
  - Easy to inspect/debug
  - Natural deduplication (one file per hash)
  - Atomic writes
- Cons:
  - Many small files (inode overhead)
  - Directory listing can be slow with 100k+ files

**Option B: Single JSONL File**
- Pros:
  - Single file, fast iteration
  - Append-only
- Cons:
  - Need index for lookups
  - Duplicate entries possible
  - Harder to deduplicate

**Option C: SQLite Database**
- Pros:
  - Efficient queries
  - Built-in indexing
  - Single file
- Cons:
  - Additional dependency
  - More complex
  - Harder to inspect manually

**Decision: Option A** - Start simple with individual files. Can migrate to SQLite later if scale becomes an issue. Add subdirectory sharding (e.g., `messages/ab/abc123.json`) if needed.

---

### Decision 3: Thread Storage Format

**Option A: JSONL (One line per entry)** ✓ RECOMMENDED
- Pros:
  - Append-only (fast writes)
  - Stream-friendly
  - Easy to tail/follow
  - Natural chronological order
- Cons:
  - No random access
  - Full scan for queries

**Option B: JSON Array**
- Pros:
  - Standard JSON
  - Random access with parsing
- Cons:
  - Must rewrite entire file on append
  - Not append-friendly
  - Memory issues with large threads

**Decision: Option A** - JSONL is ideal for append-only event logs.

---

### Decision 4: When to Log Context State

**Option A: Every LLM Request** ✓ RECOMMENDED
- Pros:
  - Complete picture of what LLM sees
  - Can replay exact context
- Cons:
  - Most verbose
  - Many duplicate references

**Option B: Only on Changes**
- Pros:
  - Minimal logging
  - Less storage
- Cons:
  - Must reconstruct context state
  - Complex replay logic

**Option C: Periodic Snapshots**
- Pros:
  - Balance of detail and storage
- Cons:
  - May miss important states
  - Complex to determine intervals

**Decision: Option A** - Log every request. Storage is cheap, message deduplication handles the bulk. Thread entries are small (just IDs and positions).

---

### Decision 5: Callback Integration Pattern

**Option A: Koog EventHandler Feature** ✓ RECOMMENDED
- Pros:
  - Native to existing framework
  - Clean separation
  - Follows existing patterns
- Cons:
  - Coupled to Koog specifics

**Option B: Wrapper/Decorator Pattern**
- Pros:
  - Framework agnostic
  - Easy to test
- Cons:
  - More boilerplate
  - May miss internal events

**Option C: AOP/Interceptors**
- Pros:
  - No code changes to core
- Cons:
  - Magic
  - Hard to debug
  - Not idiomatic Kotlin

**Decision: Option A** - Follow existing `EventHandler` pattern. Add a parallel `InteractionLogging` feature.

---

### Decision 6: Message Metadata Storage

**Option A: Embedded in Message** ✓ RECOMMENDED
- Pros:
  - Single read for all data
  - Simple model
- Cons:
  - Larger files
  - Some metadata may vary per occurrence

**Option B: Separate Metadata Files**
- Pros:
  - Smaller message files
  - Can update metadata independently
- Cons:
  - Two reads per message
  - Complex

**Decision: Option A** - Embed metadata. If something varies per occurrence, it belongs in ThreadEntry, not Message.

---

## Storage Estimates

Assumptions:
- Average message: 500 bytes
- Average thread entry: 150 bytes
- Typical session: 50 unique messages, 200 thread entries

Per session:
- Messages: 50 × 500 = 25 KB
- Thread: 200 × 150 = 30 KB
- Total: ~55 KB per session

Per day (10 sessions): ~550 KB
Per month: ~16 MB
Per year: ~200 MB

With deduplication across sessions (system prompts, common messages):
- Expect 30-50% reduction over time

**Conclusion**: Storage is very manageable.

---

## Configuration

```kotlin
data class InteractionLoggingConfig(
    val enabled: Boolean = true,
    val logDirectory: Path = Path("logs/interactions"),
    val retentionDays: Int? = null,  // null = keep forever
    val logContextOnEveryRequest: Boolean = true,
    val includeTokenEstimates: Boolean = true,
    val shardMessages: Boolean = false  // Use subdirectories for messages
)
```

---

## Future Enhancements (Out of Scope)

1. **Log Viewer CLI** - `brainiac replay <thread-id>`
2. **Log Export** - Export to OpenTelemetry format
3. **Log Compression** - Gzip old thread files
4. **Log Search** - Full-text search across messages
5. **Log Analytics** - Token usage, latency tracking
6. **Remote Logging** - Ship logs to external service

---

## Files to Create/Modify

### New Files
- `interaction-logging/build.gradle.kts` - Module setup
- `interaction-logging/src/jvmMain/kotlin/InteractionLogger.kt` - Interface
- `interaction-logging/src/jvmMain/kotlin/InteractionLoggerImpl.kt` - Implementation
- `interaction-logging/src/jvmMain/kotlin/MessageStore.kt` - Message storage
- `interaction-logging/src/jvmMain/kotlin/ThreadStore.kt` - Thread storage
- `interaction-logging/src/jvmMain/kotlin/models/LoggedMessage.kt` - Data class
- `interaction-logging/src/jvmMain/kotlin/models/ThreadEntry.kt` - Data class
- `interaction-logging/src/jvmMain/kotlin/config/InteractionLoggingConfig.kt` - Config

### Modified Files
- `settings.gradle.kts` - Add new module
- `core-agent/build.gradle.kts` - Add dependency
- `core-agent/src/jvmMain/kotlin/.../CoreAgent.kt` - Add logging hooks
- `cli/src/main/kotlin/.../Main.kt` - Add CLI flag
- `cli/src/main/kotlin/.../BrainiacPresenter.kt` - Wire up logger

---

## Summary

This design provides:
1. **Storage efficiency** via content-hash deduplication
2. **Complete traceability** via thread entries with positions
3. **Modularity** via callback-based integration
4. **Simplicity** via JSONL and individual files
5. **Extensibility** via clean interfaces

The main tradeoff is choosing simplicity (files) over query power (SQLite). This can be revisited if scale demands it.
