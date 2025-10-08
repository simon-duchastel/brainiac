package com.duchastel.simon.brainiac.core.process.nodes.util

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import kotlin.reflect.typeOf

/**
 * Helper function for passing through the input to a node as its output. [innerNode] effectively
 * acts as a side effect in the graph.
 */
inline fun <reified Input> passthrough(
    innerNode: AIAgentNodeDelegate<Input, Unit>
): AIAgentNodeDelegate<Input, Input> {
    return AIAgentNodeDelegate(
        name = innerNode.name,
        inputType = typeOf<Input>(),
        outputType = typeOf<Input>(),
        execute = { input ->
            innerNode.execute(this, input)
            input
        }
    )
}

/**
 * Helper function for passing through the input to a node as its output. [innerNode] effectively
 * acts as a side effect in the graph.
 */
inline fun <reified Input> passthrough(
    innerNode: AIAgentNodeDelegate<Unit, Unit>
): AIAgentNodeDelegate<Input, Input> {
    return AIAgentNodeDelegate(
        name = innerNode.name,
        inputType = typeOf<Input>(),
        outputType = typeOf<Input>(),
        execute = { input ->
            innerNode.execute(this, Unit)
            input
        }
    )
}