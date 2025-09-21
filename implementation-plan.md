# Brainiac Implementation Plan

This document outlines the implementation plan for the Brainiac AI Memory System, based on `spec-v1.md`. The system will be built in Kotlin, focusing on modularity and decoupling.

## 1. Project Structure

The project will be organized into a hierarchical multi-module Gradle project to ensure clear separation of concerns.

```
brainiac/
├── build.gradle.kts
├── settings.gradle.kts
├── app/                  # Main application runner (e.g., for a REST API)
│   └── src/
├── core/                 # Parent module for all core logic
│   ├── model/            # Core data classes (Memory, LTMFile, etc.)
│   ├── fs/               # Filesystem service
│   ├── llm/              # LLM service
│   ├── search/           # Search service
│   └── process/          # The four core processes
├── llm-adapter/          # Interface and implementations for LLM providers
│   └── src/
└── utils/                # Shared utility functions
    └── src/
```

## 2. Core Components

### 2.1. Data Models (`core:model`)

These are Kotlin `data class` representations of the core concepts in the spec. They will reside in the `core/model` module.

*   **`LTMFile.kt`**: Represents a file in Long-Term Memory.
    ```kotlin
    data class LTMFrontmatter(
        val uuid: String,
        val createdAt: Instant,
        val updatedAt: Instant,
        val tags: List<String>,
        var reinforcementCount: Int
    )

    data class LTMFile(
        val frontmatter: LTMFrontmatter,
        val content: String // Markdown content
    )
    ```
*   **`ShortTermMemory.kt`**: Represents the `short_term.md` file.
    ```kotlin
    data class StructuredData(
        val goals: List<String>,
        val keyFacts: List<String>,
        val tasks: List<String>
    )

    data class Event(
        val timestamp: Instant,
        val user: String,
        val ai: String,
        val thoughts: String
    )

    data class ShortTermMemory(
        val summary: String,
        val structuredData: StructuredData,
        val eventLog: List<Event>
    )
    ```
*   **`CoreIdentity.kt`**: Represents `core_identity.md`.
*   **`AccessLogEntry.kt`**: Represents a line in `access.log`.

### 2.2. FileSystem Service (`core:fs`)

A centralized service in the `core/fs` module to handle all interactions with the file system. This is critical for maintaining consistency and preventing race conditions.

*   **`FileSystemService.kt`**
    *   `read(path: Path): String`
    *   `write(path: Path, content: String)`
    *   `readLtmFile(path: Path): LTMFile` (handles parsing frontmatter)
    *   `writeLtmFile(path: Path, ltmFile: LTMFile)`
    *   `readStm(): ShortTermMemory`
    *   `writeStm(stm: ShortTermMemory)`
    *   `acquireLock(path: Path)` and `releaseLock(path: Path)`: These methods will use file-system-level locks (e.g., `java.nio.channels.FileChannel.lock()`) to ensure atomic operations on `short_term.md`.
    *   `logAccess(action: String, path: Path)`

### 2.3. LLM Service (`core:llm`)

Located in the `core/llm` module, this service provides an abstraction for interacting with an LLM.

*   **`LLMProvider.kt`** (Interface in `llm-adapter`)
    ```kotlin
    interface LLMProvider {
        fun generate(prompt: String): String
    }
    ```
*   Implementations for different providers (e.g., `OpenAIProvider.kt`, `ClaudeProvider.kt`) in `llm-adapter`.
*   **`LLMService.kt`** (`core:llm`): A service that uses a configured `LLMProvider` to perform tasks like generating search queries, summarizing text, and creating LTM content. This service will be responsible for constructing the specific prompts required by the core processes.

### 2.4. Search Service (`core:search`)

Located in the `core/search` module, this service is responsible for searching the LTM.

*   **`SearchService.kt`**
    *   `searchLTM(queries: List<String>): List<LTMFile>`
    *   Initially, this can be implemented with a simple file-based search (e.g., using `ripgrep` or a similar tool via `run_shell_command`, or a native Kotlin implementation).
    *   For a more advanced implementation, this service could build and query an in-memory index of the LTM `_index.md` files.

## 3. Core Processes (`core:process`)

Each of the four processes from the spec will be implemented as a class in the `core/process` module.

### 3.1. `CoreLoopProcess.kt`

*   **Execution:** Synchronous.
*   **Dependencies:** `FileSystemService`, `LLMService`, `SearchService`.
*   **Logic:**
    1.  Receives a user prompt.
    2.  Constructs the initial context for search query generation.
    3.  Uses `LLMService` to generate search queries.
    4.  Uses `SearchService` to retrieve LTM excerpts.
    5.  Assembles the final Working Memory.
    6.  Uses `LLMService` to generate the AI response.
    7.  Returns the response.

### 3.2. `ReflectionProcess.kt`

*   **Execution:** Asynchronous (runs in a separate coroutine).
*   **Dependencies:** `FileSystemService`.
*   **Logic:**
    1.  Triggered after the `CoreLoopProcess` completes.
    2.  Acquires a lock on `short_term.md` via `FileSystemService`.
    3.  Appends the new event to the `Event Log`.
    4.  Updates `Structured Data` if necessary.
    5.  Releases the lock.

### 3.3. `PromotionProcess.kt`

*   **Execution:** Idle-time (background scheduler).
*   **Dependencies:** `FileSystemService`, `LLMService`.
*   **Logic:**
    1.  Triggered by user inactivity or STM size limit.
    2.  Acquires a lock on `short_term.md`.
    3.  Uses `LLMService` to analyze the `Event Log` and identify promotion candidates.
    4.  For each candidate, synthesizes new or updated LTM entries.
    5.  Writes changes to LTM via `FileSystemService`, updating `_index.md` files.
    6.  Uses `LLMService` to generate a new STM summary.
    7.  Overwrites `short_term.md` with the new summary and pruned data.
    8.  Releases the lock.

### 3.4. `OrganizationProcess.kt`

*   **Execution:** Idle-time (background scheduler, e.g., every 4 hours).
*   **Dependencies:** `FileSystemService`, `LLMService`.
*   **Logic:**
    1.  Reads and parses `access.log`.
    2.  Uses `LLMService` to identify access patterns.
    3.  Proposes and executes refactoring operations on the LTM structure.
    4.  Clears `access.log`.

## 4. Concurrency and Scheduling

*   **Coroutines:** Kotlin Coroutines will be used to manage the different execution tiers.
    *   The `CoreLoopProcess` will run on a dispatcher tied to the main request thread (e.g., `Dispatchers.IO`).
    *   The `ReflectionProcess` will be launched in a separate coroutine scope (`GlobalScope.launch` or a dedicated application scope).
    *   The `PromotionProcess` and `OrganizationProcess` will be managed by a background scheduler (e.g., `java.util.concurrent.ScheduledThreadPoolExecutor` or a Kotlin-native library like `kotlinx-coroutines-core`'s `delay`).

## 5. Configuration

A simple configuration file (e.g., `config.properties` or `config.yml`) will be used to manage:
*   File paths for the `system`, `memory`, and `logs` directories.
*   LLM provider details (API keys, model names).
*   Trigger thresholds (e.g., STM token limit, inactivity timeouts).

This plan provides a solid foundation for building the Brainiac system. The next step would be to start implementing the `core/model` and `core/service` components.

