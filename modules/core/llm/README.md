# Core LLM Module

LLM interaction abstractions and working memory management for the Braniac AI Memory System.

## Why Separate LLM Module

- **Abstraction**: Decouples core system logic from specific LLM implementation details
- **Working Memory**: Manages the ephemeral context window assembly at runtime
- **Provider Independence**: Enables switching between different LLM providers
- **Context Management**: Handles token-efficient memory construction for optimal performance

## Key Design Decisions

- **Runtime Assembly**: Working memory is constructed dynamically from core identity, STM, and relevant LTM excerpts
- **Token Efficiency**: Implements intelligent context management to maximize relevant information within token limits
- **Search Integration**: Coordinates with search module to retrieve contextually relevant memories
- **Provider Agnostic**: Core logic remains independent of specific LLM API implementations

This module bridges the gap between the memory system's file-based storage and the LLM's context requirements, ensuring optimal information flow for intelligent responses.