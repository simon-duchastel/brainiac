# Fallback Module

A self-contained Gradle module that provides a koog subgraph for finding and selecting free inference AI models from [OpenRouter](https://openrouter.ai).

## Overview

This module allows you to automatically discover and select the best available free AI model from OpenRouter's catalog. It uses AI (configured via the `FallbackContext`) to intelligently analyze model metadata and choose the most suitable model based on your criteria.

## Features

- **API-based model discovery**: Fetches live model data from OpenRouter's `/api/v1/models` endpoint
- **Free model filtering**: Automatically filters for models with zero cost (pricing = "0")
- **Intelligent selection**: Uses AI to analyze model capabilities and select the best option
- **Configurable criteria**: Customize selection based on context length, capabilities, and providers
- **Self-contained**: No dependencies on other Brainiac modules (except settings.gradle.kts)

## Architecture

The module consists of three main components:

1. **OpenRouterModelsClient**: HTTP client for fetching model data from OpenRouter API
2. **FallbackModelSelector**: Koog subgraph that performs the model selection process
3. **FallbackModelStrategy**: Complete koog strategies for standalone execution

## Usage

### Basic Usage with Strategy

```kotlin
import com.duchastel.simon.brainiac.fallback.*
import ai.koog.agents.chat.llm.model.LLModel

// 1. Create the fallback context
val fallbackContext = FallbackContext(
    selectionModel = LLModel(...),  // Your AI model for selection
    openRouterClient = OpenRouterModelsClient()
)

// 2. Create and execute the strategy
with(fallbackContext) {
    val strategy = fallbackModelSelectionStrategy(
        name = "find_free_model"
    )

    // Execute with custom criteria
    val selectedModel = agent.execute(
        strategy = strategy,
        input = ModelSelectionCriteria(
            preferredCapabilities = listOf("chat", "reasoning", "code generation"),
            minimumContextLength = 8192,
            preferredProviders = listOf("anthropic", "openai")
        )
    )

    println("Selected model: ${selectedModel.name}")
    println("Model ID: ${selectedModel.id}")
    println("Context length: ${selectedModel.contextLength}")
}
```

### Quick Selection with Defaults

```kotlin
with(fallbackContext) {
    val strategy = quickFallbackModelStrategy()

    // Execute with default criteria (no input needed)
    val model = agent.execute(strategy, Unit)

    println("Selected model: ${model.name}")
}
```

### Using as a Subgraph

You can integrate the fallback model selector as a subgraph within your own koog graphs:

```kotlin
context(fallbackContext: FallbackContext)
fun myCustomStrategy(): AIAgentGraphStrategy<Unit, MyOutput> = strategy("my_strategy") {
    // Your nodes...
    val prepareInput by node<Unit, ModelSelectionCriteria>("prepare") {
        ModelSelectionCriteria(
            preferredCapabilities = listOf("chat"),
            minimumContextLength = 4096
        )
    }

    // Use the fallback selector as a subgraph
    val selectFallbackModel by selectFallbackModel(
        name = "fallback_selector"
    )

    val useSelectedModel by node<OpenRouterModel, MyOutput>("use_model") { model ->
        // Do something with the selected model
        // ...
    }

    // Wire it up
    edge(nodeStart forwardTo prepareInput)
    edge(prepareInput forwardTo selectFallbackModel)
    edge(selectFallbackModel forwardTo useSelectedModel)
    edge(useSelectedModel forwardTo nodeFinish)
}
```

## Model Selection Process

The subgraph follows this workflow:

1. **Store Criteria**: Saves the selection criteria for later use
2. **Fetch Free Models**: Calls OpenRouter API to get all models with zero pricing
3. **Filter Models**: Applies basic filters (context length, preferred providers)
4. **Check Availability**: Ensures models are available
5. **Format for Analysis**: Creates a structured summary of available models
6. **AI Selection**: Uses AI to analyze models and select the best one
7. **Extract Result**: Returns the selected model object

## Configuration Options

### ModelSelectionCriteria

Configure the selection criteria to match your needs:

```kotlin
data class ModelSelectionCriteria(
    // Capabilities you want the model to have
    val preferredCapabilities: List<String> = listOf(
        "chat",
        "reasoning",
        "code generation",
        "structured output"
    ),

    // Minimum context window size (in tokens)
    val minimumContextLength: Int = 4096,

    // Preferred model providers (e.g., "anthropic", "openai", "meta")
    val preferredProviders: List<String> = emptyList()
)
```

### FallbackContext

The context object configures how the subgraph operates:

```kotlin
data class FallbackContext(
    // The AI model used to select the best fallback model
    // This could be a local model or any model you have configured
    val selectionModel: LLModel,

    // HTTP client for OpenRouter API
    val openRouterClient: OpenRouterModelsClient
)
```

## OpenRouter Model Data

The module works with the following model metadata from OpenRouter:

- **id**: Unique model identifier (e.g., "anthropic/claude-2:free")
- **name**: Human-readable model name
- **description**: Model description and capabilities
- **contextLength**: Maximum context window size
- **pricing**: Cost structure (prompt, completion, image, request)
- **topProvider**: Provider-specific details (max tokens, moderation)
- **architecture**: Technical details (modality, tokenizer, instruct type)

## Dependencies

The module uses:

- **Koog Agents**: For graph/subgraph DSL
- **Ktor Client**: For HTTP requests to OpenRouter API
- **Kotlinx Serialization**: For JSON parsing
- **Kotlinx Coroutines**: For async operations
- **SLF4J**: For logging

## Example Output

When executed, the subgraph returns an `OpenRouterModel` object:

```kotlin
OpenRouterModel(
    id = "anthropic/claude-2:free",
    name = "Anthropic: Claude v2 (free)",
    description = "Claude 2 is a powerful AI assistant...",
    contextLength = 100000,
    pricing = ModelPricing(
        prompt = "0",
        completion = "0"
    ),
    topProvider = TopProvider(
        contextLength = 100000,
        maxCompletionTokens = 4096,
        isModerated = true
    )
)
```

## Error Handling

The module will throw errors in these cases:

- No free models available matching criteria
- Selected model ID not found in fetched models
- Network errors when fetching from OpenRouter API

## Testing

Run tests with:

```bash
./gradlew :fallback:jvmTest
```

## Integration

This module is completely self-contained. To add it to your build:

1. It's already added to `settings.gradle.kts`
2. Use it in your code by adding a dependency:

```kotlin
// In your module's build.gradle.kts
dependencies {
    implementation(project(":fallback"))
}
```

## License

Same as parent Brainiac project.
