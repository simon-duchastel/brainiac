package com.duchastel.simon.brainiac.fallback

import ai.koog.agents.*
import ai.koog.agents.chat.llm.model.LLModel

/**
 * Creates a complete koog strategy for selecting a fallback free model from OpenRouter.
 *
 * This strategy can be executed as a standalone graph to find and return the best free
 * inference model available on OpenRouter based on specified criteria.
 *
 * Example usage:
 * ```kotlin
 * val fallbackContext = FallbackContext(
 *     selectionModel = LLModel(...), // Your selection model
 *     openRouterClient = OpenRouterModelsClient()
 * )
 *
 * with(fallbackContext) {
 *     val strategy = fallbackModelSelectionStrategy(
 *         name = "find_free_model"
 *     )
 *
 *     // Execute the strategy
 *     val selectedModel = agent.execute(
 *         strategy = strategy,
 *         input = ModelSelectionCriteria(
 *             preferredCapabilities = listOf("chat", "reasoning"),
 *             minimumContextLength = 8192
 *         )
 *     )
 * }
 * ```
 *
 * @param name Name for the strategy
 * @return A koog graph strategy that takes ModelSelectionCriteria as input and returns OpenRouterModel
 */
context(fallbackContext: FallbackContext)
fun fallbackModelSelectionStrategy(
    name: String = "fallback_model_selection",
): AIAgentGraphStrategy<ModelSelectionCriteria, OpenRouterModel> = strategy(name) {
    // Create the subgraph delegate
    val selectModel by selectFallbackModel(
        name = "${name}_subgraph"
    )

    // Wire up the graph
    edge(nodeStart forwardTo selectModel)
    edge(selectModel forwardTo nodeFinish)
}

/**
 * Convenience function to fetch and select a free model with default criteria.
 *
 * This is a simpler alternative to using the full strategy when you just want
 * to quickly get a free model with reasonable defaults.
 *
 * Example usage:
 * ```kotlin
 * val fallbackContext = FallbackContext(...)
 * with(fallbackContext) {
 *     val strategy = quickFallbackModelStrategy()
 *     // Execute with default criteria
 *     val model = agent.execute(strategy, Unit)
 * }
 * ```
 *
 * @param name Name for the strategy
 * @param defaultCriteria Default selection criteria to use
 * @return A koog graph strategy that takes Unit as input and returns OpenRouterModel
 */
context(fallbackContext: FallbackContext)
fun quickFallbackModelStrategy(
    name: String = "quick_fallback_model",
    defaultCriteria: ModelSelectionCriteria = ModelSelectionCriteria(
        preferredCapabilities = listOf(
            "chat",
            "reasoning",
            "code generation",
            "structured output"
        ),
        minimumContextLength = 4096,
    ),
): AIAgentGraphStrategy<Unit, OpenRouterModel> = strategy(name) {
    val criteriaKey = createStorageKey<ModelSelectionCriteria>("${name}_criteria")

    // Node to set default criteria
    val setDefaultCriteria by node<Unit, ModelSelectionCriteria>("${name}_set_defaults") {
        storage.set(criteriaKey, defaultCriteria)
        defaultCriteria
    }

    // Create the subgraph delegate
    val selectModel by selectFallbackModel(
        name = "${name}_subgraph"
    )

    // Wire up the graph
    edge(nodeStart forwardTo setDefaultCriteria)
    edge(setDefaultCriteria forwardTo selectModel)
    edge(selectModel forwardTo nodeFinish)
}
