# Braniac: An AI Memory System Specification

## 1. Overview & Guiding Principles

This document specifies the architecture for Braniac, an artificial intelligence system designed for persistent memory and continuous learning. The primary goal is to create an intelligence that evolves through its interactions, building a unique and useful internal model of its world and its purpose.

The design of this system is guided by the following core principles:

*   **File-System Centric:** The system's memory is to be stored in a transparent, hierarchical structure of plain-text files. This ensures data longevity, interoperability, and human-readability, explicitly avoiding opaque formats or proprietary databases.

*   **Human-Inspired Architecture:** The memory system is modeled loosely on the human brain, incorporating distinct tiers for working, short-term, and long-term memory. This provides a robust framework for information processing, from immediate consciousness to deep, consolidated knowledge.

*   **Autonomous & Emergent Intelligence:** The system manages its own memory automatically. It decides what to remember, what to forget, and how to organize its knowledge based on its experiences. Characteristics like the "importance" of a memory are not defined by explicit scores but are an emergent property of the memory's location, connectivity, and access frequency within the system.

## 2. System Architecture

The system is composed of three core storage directories (`system`, `memory`, `logs`) and four primary processes that manage the flow of information between them.

### 2.1. Directory Structure

```
/
├── system/
│   └── core_identity.md
├── memory/
│   ├── short_term/
│   └── long_term/
└── logs/
    └── access.log
```

### 2.2. Information Flow

The processes create a continuous loop of interaction, reflection, and consolidation:

1.  **Core Loop (Interaction):** Handles real-time interaction with the user. Reads from LTM to inform responses.
2.  **Reflection:** Captures key information from the Core Loop into Short-Term Memory (STM).
3.  **Promotion:** Consolidates memories from STM into Long-Term Memory (LTM).
4.  **Organization:** Refactors and evolves the structure of LTM.

```
User <--> [Core Loop] <--> LTM
             |
             v
      [Reflection] --> STM --> [Promotion] --> LTM <--> [Organization]
```

## 3. Memory Tiers

### 3.1. Working Memory

Working Memory is not a location on disk. It is an **ephemeral construct**, representing the LLM's active context window at the moment of processing a request. It is assembled at runtime and MUST contain:

1.  **Core Identity:** The foundational goals and principles from `system/core_identity.md`.
2.  **Conversation Summary:** A rolling, token-efficient summary of the current interaction.
3.  **Relevant LTM Excerpts:** One or more memory files retrieved from LTM that are relevant to the current user prompt.
4.  **The User Prompt:** The immediate query or instruction from the user.

### 3.2. Short-Term Memory (STM)

The staging area for new memories. It is a single, consolidated document that functions as a cognitive scratchpad, capturing the system's immediate context.

*   **Location:** `memory/short_term.md`
*   **Guiding Principle:** STM is an "append-mostly" document. New events are appended to a log, preserving a granular, immutable record of recent interactions between pruning cycles. This avoids the data loss inherent in re-summarizing a summary.
*   **Structure:** The `short_term.md` file MUST be a Markdown file with the following structure:

    ```markdown
    # Short-Term Memory Scratchpad

    ## Summary
    A 1-3 paragraph, machine-generated summary of the events and data below. This summary is only updated by the Promotion & Pruning Process after it has consolidated important information into LTM.

    ---
    ## Structured Data
    This section contains discrete, machine-readable data for immediate use.

    ### Goals
    - [ ] A list of the user's current, active goals.

    ### Key Facts & Decisions
    - A list of important facts or decisions made in the recent past.

    ### Tasks
    - [ ] A list of pending tasks for the system to complete.

    ---
    ## Event Log
    A reverse-chronological log of recent interactions. New events are appended to the top.

    ### 2025-09-16T15:30:00Z
    **User:** "How do I time it?"
    **AI:** "To time a Python function, you can use..."
    **Thoughts:** The user is asking about performance in the context of decorators. I should focus on `timeit` and wrapper functions.

    ### 2025-09-16T15:25:00Z
    **User:** "Tell me about Python decorators."
    **AI:** "A decorator is a function that takes another function as an argument..."
    **Thoughts:** This is a foundational programming concept.
    ```

### 3.3. Long-Term Memory (LTM)

The permanent, structured, and evolving knowledge base of the system.

*   **Location:** `memory/long_term/`
*   **Structure:** A hierarchical directory structure. The hierarchy itself is a form of knowledge representation. Deeper nesting implies more specific or nuanced concepts.
*   **Memory Types:** LTM is organized into three primary top-level directories:
    *   `concrete/`: For semantic memory. Contains facts and knowledge about specific topics, concepts, and entities (e.g., `/concrete/programming/python/`).
    *   `events/`: For episodic memory. Contains records of interactions and actions taken by the system, organized chronologically (e.g., `/events/2025/09/16/`).
    *   `skills/`: For procedural memory. Contains structured "recipes" or sequences of actions that lead to a successful outcome for a given task. Like other LTM directories, this is a hierarchical structure with `_index.md` files to facilitate search and organization (e.g., `/skills/refactoring/python/`).
*   **File Format:** All memory files MUST be Markdown files with YAML frontmatter.
    *   **Frontmatter:**
        *   `uuid`: A unique, immutable identifier for the memory file. Essential for stable linking.
        *   `created_at`: The ISO 8601 timestamp of the memory's creation.
        *   `updated_at`: The ISO 8601 timestamp of the memory's last modification.
        *   `tags`: A list of relevant keywords or categories (e.g., `[python, webdev, fastapi]`).
        *   `emotion`: A single, descriptive tag for the affective context of the memory (e.g., `curiosity`, `satisfaction`).
    *   **Body:** The content of the memory in Markdown.
*   **Indexing & Relationality (`_index.md`):** This is the core mechanism for search and establishing relationships.
    *   **Requirement:** Every directory inside LTM MUST contain an `_index.md` file.
    *   **Purpose:** To provide a summary of the directory's concept and to link it to other memories, forming a semantic web.
    *   **Content:** The `_index.md` file MUST contain:
        1.  A **summary** of the topic the directory represents.
        2.  A **manifest** of the memory files and subdirectories it contains.
        3.  A **"Related Memories"** section with explicit, UUID-based links to other relevant memory files or `_index.md` files throughout the LTM.

## 4. Core Processes

### 4.1. Execution Model

The system's processes operate on three distinct tiers to ensure responsiveness:

*   **Synchronous (Foreground):** The Core Loop, which must execute in real-time while the user is waiting.
*   **Asynchronous (Near-Background):** The Reflection process, which runs in parallel immediately after a user interaction, without blocking the next prompt.
*   **Idle (Far-Background):** The Promotion and Organization processes, which are heavier and designed to run only when the system is not actively engaged with the user.

### 4.2. The Core Loop (Interaction Process)

1.  Receive user prompt.
2.  The system assembles the Working Memory context. This MUST include:
    *   Core Identity (`system/core_identity.md`)
    *   The User Prompt
    *   The **entire content** of `memory/short_term.md`. This file serves as the primary source of immediate context.
    *   Relevant LTM excerpts retrieved in the next step.
3.  The LLM generates a set of search queries for LTM based on the full context assembled in the previous step, including the rich data from `short_term.md`.
4.  The system retrieves the most relevant memory files based on the search results.
5.  The LLM generates a response based on the assembled context.
6.  For every LTM file read in step 4, an entry is appended to `logs/access.log`.
7.  The response is delivered to the user.

### 4.3. The Reflection Process (Working -> STM)

*   **Trigger:** Asynchronous, immediately after step 8 of the Core Loop.
*   **Action:** The process MUST acquire an exclusive file lock on `memory/short_term.md` to ensure atomic updates. It then **appends** a new, timestamped entry to the `## Event Log` section. It may also update the lists within the `## Structured Data` section (e.g., adding a new task or fact). It MUST NOT modify the `## Summary` section. After the write is complete, the lock is released.

### 4.4. The Promotion & Pruning Process (STM -> LTM)

*   **Trigger:** Idle-time (e.g., no user activity for 5 minutes).
*   **Action:** This idle-time process is now a multi-step workflow:
    1.  **Acquire Lock:** The process acquires an exclusive lock on `memory/short_term.md`.
    2.  **Analyze:** The LLM reads the entire `## Event Log`. Its goal is to identify recurring themes, important entities, and concepts suitable for long-term storage. It outputs a list of these "promotion candidates" and the event timestamps related to each.
    3.  **Synthesize:** For each major "promotion candidate," the system makes a focused LLM call, providing the relevant log excerpts and instructing it to write a new, coherent, and consolidated LTM entry (as either a `concrete`, `event`, or `skill` memory).
    4.  **Write to LTM:** The newly synthesized entries are written to their final destination in the `memory/long_term/` directory, with `_index.md` files being updated accordingly.
    5.  **Prune & Summarize:** After all candidates have been promoted, the LLM is called one last time. It is given the list of events that were just promoted. Its prompt is: "The following events were just archived to LTM. For the remaining, non-archived events in the log, generate a new, concise 1-3 paragraph summary. The old summary is now obsolete."
    6.  **Finalize:** The `memory/short_term.md` file is completely overwritten. The new content consists of the newly generated summary, empty `Structured Data` lists, and an empty `Event Log`. This resets the STM for the next cycle, carrying over only a high-level summary of what was left behind. The lock is then released.

### 4.5. The Organization Process (LTM Evolution)

*   **Trigger:** Long idle-time or a scheduled nightly task.
*   **Action:** This is the system's "deep thought" cycle.
    1.  Read and parse the `logs/access.log` file.
    2.  Use the LLM to identify access patterns (e.g., frequently used memories, co-accessed memories, unused memories).
    3.  Based on these patterns, the LLM proposes and executes refactoring operations:
        *   **Strengthen Relations:** Add new links between concepts in `_index.md` files.
        *   **Restructure:** Move frequently accessed memories to higher, more accessible locations in the hierarchy.
        *   **Archive:** Compress or move memories that have not been accessed for a prolonged period.
    4.  Clear the `logs/access.log` file after it has been processed.

## 5. System & Logging

### 5.1. Core Identity

The `system/core_identity.md` file serves as the AI's constitution. It defines its foundational purpose, operational guidelines, ethical constraints, and core personality traits. This file MUST be loaded into Working Memory for every task to ensure all actions are aligned with its core principles.

### 5.2. Access Logging

The `logs/access.log` file is a simple, machine-readable log that records every time a file in LTM is read.

*   **Purpose:** To provide the Organization Process with the necessary data to identify usage patterns and evolve the LTM structure.
*   **Format:** `[ISO_8601_TIMESTAMP] | [ACTION] | [ABSOLUTE_FILE_PATH]`
*   **Example:** `2025-09-16T15:30:00Z | READ | /path/to/project/memory/long_term/concrete/programming/python.md`


