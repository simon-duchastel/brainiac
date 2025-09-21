# Core Module

The foundational modules implementing the core AI memory system functionality as specified in the Braniac architecture.

## Why Core Module Division

- **Domain Separation**: Each sub-module handles a distinct aspect of the memory system
- **Dependency Management**: Clear boundaries prevent circular dependencies and enable focused testing
- **Cognitive Architecture**: Mirrors the specification's separation of memory, processing, and I/O concerns
- **Scalability**: Individual core components can evolve independently

## Sub-modules

- `fs/`: File system operations for transparent, hierarchical memory storage
- `llm/`: LLM interaction abstractions and working memory management
- `model/`: Domain models for memory structures, metadata, and system entities
- `process/`: The four core processes (Core Loop, Reflection, Promotion, Organization)
- `search/`: Memory retrieval and indexing functionality

This modular approach implements the specification's file-system centric, human-inspired architecture while maintaining clean separation between storage, processing, and interaction concerns.