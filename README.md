# Brainiac AI Memory System

An artificial intelligence system designed for persistent memory and continuous learning, implementing a human-inspired architecture with autonomous memory management.

Built with **Kotlin Multiplatform** and powered by the [Koog](https://koog.ai) agent framework.

## Overview

Brainiac is an AI system that mimics human memory architecture, featuring:
- **Working Memory**: Ephemeral context assembled at runtime
- **Short-Term Memory (STM)**: Staging area for recent interactions
- **Long-Term Memory (LTM)**: Permanent, hierarchical knowledge base

The system autonomously manages what to remember, what to forget, and how to organize its knowledge based on experiences and access patterns.

## Architecture

### Memory Tiers

1. **Working Memory** (Runtime)
   - Core identity and system prompt
   - Conversation summary
   - Relevant LTM excerpts
   - Current user prompt

2. **Short-Term Memory** (`memory/short_term.md`)
   - Append-mostly event log
   - Structured data (goals, tasks, facts)
   - Summary of recent activity

3. **Long-Term Memory** (`memory/long_term/`)
   - Hierarchical file structure
   - Three types: concrete (semantic), events (episodic), skills (procedural)
   - Indexed with `_index.md` files for semantic linking

### Core Processes

- **Core Loop**: Real-time user interaction with memory retrieval
- **Reflection**: Captures key information into STM after interactions
- **Promotion**: Consolidates important STM entries into LTM
- **Organization**: Refactors LTM structure based on access patterns

See [specs/spec-v1.md](specs/spec-v1.md) for detailed architecture documentation.

## Project Structure

```
brainiac/
├── core-loop/           # Main memory system implementation
│   └── src/
│       ├── jvmMain/
│       │   └── kotlin/
│       │       ├── CoreLoopProcess.kt          # Main process orchestrator
│       │       ├── nodes/
│       │       │   ├── LongTermMemoryNodes.kt  # LTM operations
│       │       │   ├── ShortTermMemoryNodes.kt # STM operations
│       │       │   └── util/
│       │       │       └── PassthroughNode.kt  # Helper utilities
│       │       └── graphs/
│       │           └── PromotionProcess.kt     # Memory promotion logic
│       └── jvmTest/     # Unit tests
├── docs/                # Additional documentation
├── specs/               # System specifications
└── prompts/             # LLM prompt templates
```

## Building

This project uses Gradle with Kotlin Multiplatform:

```bash
# Run tests
./gradlew test

# Build the project
./gradlew build
```

## Dependencies

- **Koog Agents**: AI agent orchestration framework
- **Kotlin Coroutines**: Asynchronous processing
- **Kotest**: Testing framework

## Current Status

This is an active development project implementing the memory system specification. The core architecture is in place with the promotion process graph operational.

## Implementation Philosophy

### File-System Centric
Memory is stored in transparent, hierarchical plain-text files for longevity, interoperability, and human-readability.

### Human-Inspired
The memory architecture models the human brain with distinct working, short-term, and long-term memory tiers.

### Autonomous Intelligence
The system manages its own memory, with importance emerging from location, connectivity, and access patterns rather than explicit scoring.

## License

See [LICENSE](LICENSE) for details.