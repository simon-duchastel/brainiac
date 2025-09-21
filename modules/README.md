# Modules

The modular architecture of Braniac AI Memory System, implementing a clean separation of concerns across functional domains.

## Why Modular Architecture

- **Maintainability**: Each module has a single responsibility, reducing complexity and making the codebase easier to understand and modify
- **Testability**: Isolated modules can be tested independently with clear interfaces
- **Reusability**: Core modules can be leveraged across different parts of the system
- **Scalability**: New functionality can be added as new modules without affecting existing code

## Module Organization

- `app/`: Application entry point and configuration
- `core/`: Foundation modules for the AI memory system core functionality  
- `llm-adapter/`: Abstraction layer for different LLM providers
- `utils/`: Shared utilities and helper functions
