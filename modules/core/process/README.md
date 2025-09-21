# Core Process Module

Implementation of the four core processes that manage information flow in the Brainiac AI Memory System.

## Why Process-Based Architecture

- **Cognitive Modeling**: Mirrors human memory processes with distinct phases for interaction, reflection, and consolidation
- **Asynchronous Design**: Separates real-time user interaction from background memory processing
- **Autonomous Memory**: Processes decide what to remember, forget, and reorganize based on usage patterns
- **Emergent Intelligence**: Memory importance emerges from access patterns rather than explicit scoring

## Implementation Status

### ✅ Implemented Processes

- **CoreLoopProcess**: Complete synchronous implementation with proper context ordering
  - Uses CoreIdentityService for extensible identity management
  - Follows least → most volatile context ordering (Core Identity → STM → LTM → User Prompt)
  - Comprehensive test coverage with edge cases
  - Clean service integration with LLMService, SearchService, and CoreIdentityService

### 🔄 Future Processes

- **Reflection (Near-Background)**: Captures interaction context into short-term memory immediately after user exchanges
- **Promotion (Idle)**: Consolidates STM events into structured LTM during inactivity periods
- **Organization (Far-Background)**: Evolves LTM structure based on access patterns and usage analytics

## The Four Core Processes

- **Core Loop (Synchronous)**: Real-time user interaction with LTM retrieval and working memory assembly
- **Reflection (Near-Background)**: Captures interaction context into short-term memory immediately after user exchanges
- **Promotion (Idle)**: Consolidates STM events into structured LTM during inactivity periods
- **Organization (Far-Background)**: Evolves LTM structure based on access patterns and usage analytics

## Dependencies

- **CoreIdentityService**: Extensible identity management
- **FileSystemService**: STM reading and LTM access logging
- **LLMService**: Search query generation and response generation
- **SearchService**: LTM retrieval based on queries

This process architecture implements the specification's human-inspired memory system with autonomous memory management and emergent organizational intelligence.