# Core Model Module

Domain models and data structures for the Braniac AI Memory System, implementing the specification's memory architecture.

## Why Domain Models

- **Type Safety**: Kotlin data classes provide compile-time guarantees for memory structure integrity
- **Specification Compliance**: Models directly implement the memory file formats and metadata defined in the specification
- **Serialization**: Structured data models enable clean YAML frontmatter and JSON serialization
- **Business Logic**: Domain objects encapsulate memory-specific behaviors and validation

## Key Models

- `LTMFile`: Long-term memory files with YAML frontmatter (UUID, timestamps, tags, reinforcement count)
- `ShortTermMemory`: STM structure with summary, structured data sections, and event log
- `CoreIdentity`: AI constitution and foundational principles
- `AccessLogEntry`: File access tracking for organizational process

These models provide the foundational data structures that implement the specification's human-inspired memory architecture while maintaining type safety and clear domain boundaries.