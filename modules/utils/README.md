# Utils Module

Shared utilities and helper functions for the Braniac AI Memory System.

## Why Shared Utils

- **DRY Principle**: Avoids code duplication across modules with common functionality
- **Consistency**: Ensures uniform behavior for shared operations like logging, validation, and serialization
- **Maintainability**: Centralizes common utilities for easier testing and updates
- **Foundation**: Provides stable building blocks for higher-level modules

## Key Utilities

- **Serialization Helpers**: YAML and JSON serialization utilities for memory file formats
- **File Operations**: Common file system utilities and path manipulation
- **Validation**: Input validation and data integrity checks
- **Logging**: Structured logging and debugging utilities

This module provides the foundational utilities that support the core system without introducing domain-specific logic, maintaining clean separation of concerns across the modular architecture.