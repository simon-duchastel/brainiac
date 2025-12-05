package com.duchastel.simon.brainiac.fallback

import ai.koog.agents.*
import ai.koog.agents.chat.llm.model.LLModel
import kotlinx.serialization.Serializable

/**
 * Context for the fallback model selector subgraph.
 *
 * @property selectionModel The LLM model to use for selecting the best free model
 * @property openRouterClient Client for fetching models from OpenRouter
 */
data class FallbackContext(
    val selectionModel: LLModel,
    val openRouterClient: OpenRouterModelsClient,
)

/**
 * Configuration for model selection criteria.
 */
data class ModelSelectionCriteria(
    val preferredCapabilities: List<String> = listOf(
        "chat",
        "reasoning",
        "code generation",
        "structured output"
    ),
    val minimumContextLength: Int = 4096,
    val preferredProviders: List<String> = emptyList(),
)

/**
 * Koog subgraph for fetching and selecting a free inference model from OpenRouter.
 *
 * This subgraph performs the following steps:
 * 1. Fetches all free models from OpenRouter API
 * 2. Filters models based on basic criteria (context length, availability)
 * 3. Uses AI to analyze and select the best model based on metadata and user criteria
 * 4. Returns the selected model
 *
 * @param name Optional name for the subgraph
 * @param selectionCriteria Criteria for selecting the best model
 * @return A subgraph delegate that takes ModelSelectionCriteria as input and returns the selected OpenRouterModel
 */
@AIAgentBuilderDslMarker
context(fallbackContext: FallbackContext)
inline fun <reified T : Any> AIAgentSubgraphBuilderBase<*, *>.selectFallbackModel(
    name: String? = null,
    selectionCriteria: ModelSelectionCriteria = ModelSelectionCriteria(),
): AIAgentSubgraphDelegate<ModelSelectionCriteria, OpenRouterModel> = subgraph(
    name = name,
    toolSelectionStrategy = ToolSelectionStrategy.NONE,
) {
    // Storage keys for passing data between nodes
    val criteriaKey = createStorageKey<ModelSelectionCriteria>("${name}_criteria")
    val freeModelsKey = createStorageKey<List<OpenRouterModel>>("${name}_free_models")
    val filteredModelsKey = createStorageKey<List<OpenRouterModel>>("${name}_filtered_models")

    // Node 1: Store the input criteria
    val storeCriteria by node<ModelSelectionCriteria, Unit>("${name}_store_criteria") { criteria ->
        storage.set(criteriaKey, criteria)
    }

    // Node 2: Fetch free models from OpenRouter
    val fetchFreeModels by node<Unit, List<OpenRouterModel>>("${name}_fetch_free_models") {
        val models = fallbackContext.openRouterClient.fetchFreeModels()
        storage.set(freeModelsKey, models)
        models
    }

    // Node 3: Filter models based on basic criteria
    val filterModels by node<List<OpenRouterModel>, List<OpenRouterModel>>("${name}_filter_models") { models ->
        val criteria = storage.getValue(criteriaKey)

        val filtered = models.filter { model ->
            // Filter by minimum context length
            val contextLength = model.contextLength ?: model.topProvider?.contextLength ?: 0
            contextLength >= criteria.minimumContextLength
        }.filter { model ->
            // Filter by preferred providers if specified
            if (criteria.preferredProviders.isEmpty()) {
                true
            } else {
                criteria.preferredProviders.any { provider ->
                    model.id.contains(provider, ignoreCase = true) ||
                    model.name.contains(provider, ignoreCase = true)
                }
            }
        }

        storage.set(filteredModelsKey, filtered)
        filtered
    }

    // Node 4: Check if we have any models
    val checkModelsAvailable by node<List<OpenRouterModel>, Boolean>("${name}_check_models") { models ->
        models.isNotEmpty()
    }

    // Node 5: Format models for AI analysis
    val formatModelsForAnalysis by node<List<OpenRouterModel>, String>("${name}_format_models") {
        val models = storage.getValue(filteredModelsKey)
        val criteria = storage.getValue(criteriaKey)

        buildString {
            appendLine("# Available Free Models from OpenRouter")
            appendLine()
            appendLine("Selection Criteria:")
            appendLine("- Preferred capabilities: ${criteria.preferredCapabilities.joinToString(", ")}")
            appendLine("- Minimum context length: ${criteria.minimumContextLength}")
            if (criteria.preferredProviders.isNotEmpty()) {
                appendLine("- Preferred providers: ${criteria.preferredProviders.joinToString(", ")}")
            }
            appendLine()
            appendLine("Available Models:")
            appendLine()

            models.forEachIndexed { index, model ->
                appendLine("## Model ${index + 1}: ${model.name}")
                appendLine("- **ID**: ${model.id}")
                model.description?.let { appendLine("- **Description**: $it") }
                val contextLength = model.contextLength ?: model.topProvider?.contextLength
                contextLength?.let { appendLine("- **Context Length**: $it tokens") }
                model.topProvider?.maxCompletionTokens?.let {
                    appendLine("- **Max Completion Tokens**: $it")
                }
                model.architecture?.modality?.let { appendLine("- **Modality**: $it") }
                model.architecture?.instructType?.let { appendLine("- **Instruct Type**: $it") }
                model.topProvider?.isModerated?.let { appendLine("- **Moderated**: $it") }
                appendLine()
            }
        }
    }

    // Node 6: Use AI to select the best model
    val selectBestModel by node<String, ModelSelection>("${name}_select_best_model") { formattedModels ->
        llm.writeSession {
            rewritePrompt {
                prompt("select_best_model") {
                    system {
                        +"""
                        You are an AI model selection expert. Your task is to analyze the available
                        free models from OpenRouter and select the best one based on the provided criteria.

                        Consider the following factors when making your selection:
                        1. Context length (larger is generally better for complex tasks)
                        2. Model capabilities matching the preferred capabilities
                        3. Model reputation and provider quality
                        4. Moderation status (unmoderated may be preferred for flexibility)
                        5. Instruct type compatibility
                        6. Max completion tokens (more is better for longer responses)

                        Analyze each model carefully and select the single best model for general-purpose
                        inference tasks. Provide your reasoning for the selection.
                        """.trimIndent()
                    }
                    user {
                        +formattedModels
                    }
                }
            }

            // Use the selection model for this decision
            withModel(fallbackContext.selectionModel) {
                requestLLMStructured<ModelSelection>().getOrNull()
                    ?: error("Failed to get structured response for model selection")
            }.structure
        }
    }

    // Node 7: Extract the selected model from the list
    val extractSelectedModel by node<ModelSelection, OpenRouterModel>("${name}_extract_model") { selection ->
        val models = storage.getValue(filteredModelsKey)

        // Find the model by ID
        models.find { it.id == selection.selectedModelId }
            ?: error("Selected model ID '${selection.selectedModelId}' not found in available models")
    }

    // Node 8: Handle no models available case
    val handleNoModels by node<Boolean, OpenRouterModel>("${name}_handle_no_models") {
        error("No free models available from OpenRouter matching the criteria")
    }

    // Define the graph flow
    edge(nodeStart forwardTo storeCriteria)
    edge(storeCriteria forwardTo fetchFreeModels)
    edge(fetchFreeModels forwardTo filterModels)
    edge(filterModels forwardTo checkModelsAvailable)

    // If models available, continue with selection
    edge(
        checkModelsAvailable forwardTo formatModelsForAnalysis
        onCondition { it }
    )
    edge(formatModelsForAnalysis forwardTo selectBestModel)
    edge(selectBestModel forwardTo extractSelectedModel)
    edge(extractSelectedModel forwardTo nodeFinish)

    // If no models available, error
    edge(
        checkModelsAvailable forwardTo handleNoModels
        onCondition { !it }
    )
    edge(handleNoModels forwardTo nodeFinish)
}

/**
 * Structured output for AI model selection.
 */
@Serializable
data class ModelSelection(
    val selectedModelId: String,
    val reasoning: String,
    val alternativeModelIds: List<String> = emptyList(),
)
