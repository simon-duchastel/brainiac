# Braniac Implementation Plan

## Architecture Overview

**Core Philosophy**: Selective AI delegation - leverage AI for intelligence, use deterministic code for infrastructure reliability. Build as modular Kotlin library with clean separation between memory engine and external interfaces.

## Module Architecture

### 1. Memory Foundation (`braniac-memory`)

**MemoryFile Models**: Strongly-typed representations of memory files with YAML frontmatter parsing. Separate models for regular memories vs index files due to different structures and requirements.

**FileSystemManager**: Direct file I/O operations with atomic write guarantees. Handles memory file format serialization/deserialization. Abstracts storage to enable testing and future storage backends.

**FileLockManager**: Exclusive file locking for STM updates as mandated by spec. Prevents corruption during concurrent access. Uses platform-specific file locking mechanisms.

**AccessLogger**: Append-only logging of every file operation with precise timestamps. Simple text format for easy parsing by organization process. Critical for usage pattern analysis.

### 2. Memory Repositories (`braniac-repositories`)

**ShortTermMemoryRepository**: Manages STM file with strict append-only semantics for event log. Handles token counting for promotion triggers. Encapsulates file locking during updates. Maintains structured data sections (goals/facts/tasks).

**LongTermMemoryRepository**: Hierarchical memory storage with automatic index file maintenance. Ensures every directory has required _index.md. Handles memory file CRUD with proper logging. Supports search by path patterns and UUID lookups.

**MemoryIndexManager**: Maintains bidirectional links between memories via UUID references. Updates index files when memories are added/modified. Provides graph traversal capabilities for related memory discovery.

### 3. AI Integration Layer (`braniac-ai`)

**AIProvider Interface**: Defines specific methods for each AI task rather than generic "ask" methods. Enables targeted prompting and response parsing for each use case.

**Selective Delegation Strategy**:
- **Delegate to AI**: Memory synthesis, search query generation, access pattern analysis, content summarization
- **Keep in Code**: File operations, access logging, token counting, timing, data validation

**PromptTemplates**: Structured, version-controlled prompts for each AI operation. Includes response format specifications for reliable parsing. Tunable for different AI providers.

**Provider Implementations**: Concrete implementations for Claude, OpenAI with provider-specific optimizations. Handles API rate limiting, retries, and error recovery.

### 4. Core Processes (`braniac-processes`)

**CoreLoop**: Synchronous user interaction handler following spec's 7-step process. Assembles working memory from core identity, STM summary, and relevant LTM excerpts. Coordinates with AI provider for response generation.

**ReflectionProcessor**: Asynchronous STM updates immediately after interactions. Uses AI to extract key facts, goals, and tasks from interaction context. Appends timestamped events to STM event log.

**PromotionProcessor**: STM→LTM consolidation triggered by idle time or token count. AI analyzes event log for promotion candidates. Searches existing LTM before creating new memories. Handles reinforcement counting and memory updates.

**OrganizationProcessor**: LTM evolution based on access log analysis. AI identifies patterns in memory usage and proposes refactoring operations. Executes hierarchy restructuring, relationship strengthening, and archival.

### 5. Search & Retrieval (`braniac-search`)

**HybridSearchEngine**: Combines multiple search strategies for optimal relevance. Text search for exact matches, index search for curated relationships, optional semantic search for conceptual similarity.

**QueryGenerator**: AI-powered search query generation from user context. Takes into account recent events, active goals, and conversation history. Generates multiple diverse queries for comprehensive coverage.

**RelevanceRanker**: Scores search results using multiple signals: text relevance, access frequency, reinforcement count, relationship proximity, recency.

### 6. Process Coordination (`braniac-scheduler`)

**ProcessScheduler**: Manages timing requirements from spec. Coordinates three execution tiers: synchronous (CoreLoop), asynchronous (Reflection), idle (Promotion/Organization). Handles promotion triggers (5min idle OR 4096 tokens).

**TokenCounter**: Precise token counting for STM using consistent methodology. Triggers promotion process when threshold exceeded. Configurable for different tokenization approaches.

**EventBus**: Decouples processes via event-driven communication. Enables process coordination without tight coupling. Supports process monitoring and debugging.

### 7. Engine Core (`braniac-engine`)

**BraniacEngine**: Main public API providing simple interface for complex orchestration. Handles dependency injection and configuration. Manages system lifecycle and resource cleanup.

**ConfigurationManager**: AI provider selection, timing parameters, file paths, token limits. Environment-specific configurations for development vs production.

**SystemMonitor**: Memory usage tracking, process performance metrics, AI API usage statistics. Health checks and system status reporting.

### 8. External Interfaces (`braniac-api`)

**REST API**: HTTP endpoints for external integration. Streaming support for real-time interactions. Authentication and rate limiting for multi-user scenarios.

**Admin API**: System monitoring, configuration management, memory export/import. Debug interfaces for development and troubleshooting.

## Key Implementation Decisions

**Memory Storage**: Plain text files in hierarchical directories as specified. YAML frontmatter with strict schema validation. Mandatory index files with relationship management.

**Concurrency Model**: Kotlin coroutines for async operations. Exclusive file locking only where required (STM updates). Lock-free reading for performance.

**AI Provider Strategy**: Abstract interface with multiple implementations. Provider-specific optimizations for different AI capabilities. Fallback mechanisms for reliability.

**Error Handling**: Graceful degradation when AI services unavailable. Retry logic with exponential backoff. Local fallbacks for critical operations.

**Testing Strategy**: MockAIProvider for deterministic testing. Test file system for isolation. Component testing with real AI providers for integration validation.

## Development Phases

**Phase 1 - Foundation (2-3 weeks)**
Memory models, file system operations, basic AI provider integration. Core Loop implementation without advanced features.

**Phase 2 - Process Engine (3-4 weeks)**
Reflection and Promotion processes with proper locking. STM management and token counting. Access logging infrastructure.

**Phase 3 - Intelligence Layer (2-3 weeks)**
Organization process with pattern analysis. Advanced search and retrieval. Memory relationship management.

**Phase 4 - API & Production (2-3 weeks)**
REST API implementation. Configuration management. Performance optimization and monitoring.

## Success Criteria

System demonstrates emergent memory importance through access patterns. Autonomous memory organization improves retrieval over time. Reliable file operations with no data corruption. Cost-effective AI usage through selective delegation.

## Component Dependencies

```
braniac-api
├── braniac-engine
    ├── braniac-scheduler
    │   ├── braniac-processes
    │   │   ├── braniac-repositories
    │   │   │   └── braniac-memory
    │   │   ├── braniac-ai
    │   │   │   └── braniac-memory
    │   │   └── braniac-search
    │   │       ├── braniac-repositories
    │   │       └── braniac-ai
    │   └── braniac-repositories
    ├── braniac-processes
    ├── braniac-search
    ├── braniac-ai
    ├── braniac-repositories
    └── braniac-memory
```

## Critical Implementation Notes

**File Format Compliance**: Every LTM file must have exact YAML frontmatter structure with uuid, timestamps, tags, reinforcement_count. Index files must contain summary, manifest, and related memories sections.

**Locking Strategy**: Only STM updates require exclusive file locking. All other operations use optimistic concurrency. Access logging must be atomic and never block other operations.

**AI Integration Points**: Search query generation, memory synthesis, access pattern analysis, content summarization. Each requires specific prompt templates and response parsing logic.

**Performance Considerations**: Token counting triggers promotion at 4096 tokens. Access logging overhead must be minimal. Search operations should be sub-second for typical queries.

**Reliability Requirements**: System must handle AI service outages gracefully. File corruption prevention is critical. Memory data must never be lost during process failures.