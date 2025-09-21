# App Module

Main application entry point and orchestration layer for the Braniac AI Memory System.

## Why Separate App Module

- **Composition Root**: Centralizes dependency injection and application configuration
- **Clean Architecture**: Separates application logic from core business logic
- **Entry Point**: Provides a single, well-defined starting point for the entire system
- **Integration Layer**: Orchestrates interactions between all core modules without coupling them

## Dependencies

The app module integrates all core system components:
- `core:process` - Memory processes (reflection, promotion, organization)
- `core:model` - Data models and domain objects
- `core:fs` - File system operations for memory storage
- `core:llm` - LLM interaction abstractions
- `core:search` - Memory search and retrieval
- `llm-adapter` - LLM provider implementations

This module contains no business logic itself, serving purely as the application composition and startup layer.