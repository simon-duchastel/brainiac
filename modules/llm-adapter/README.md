# LLM Adapter Module

Provider abstraction layer for Large Language Model integrations in the Brainiac AI Memory System.

## Why Adapter Pattern

- **Provider Independence**: System logic remains decoupled from specific LLM APIs and implementations
- **Extensibility**: New LLM providers can be added without modifying core system code
- **Configuration Management**: Centralized provider configuration and credential management
- **API Normalization**: Consistent interface regardless of underlying provider differences

## Key Design Decisions

- **Strategy Pattern**: Different LLM providers implement a common interface
- **Configuration-Driven**: Provider selection and parameters controlled through configuration
- **Error Handling**: Unified error handling and retry logic across all providers
- **Token Management**: Provider-agnostic token counting and context window management

This adapter layer enables the core system to work with multiple LLM providers (OpenAI, Anthropic, local models) while maintaining a clean separation between business logic and external API integration concerns.