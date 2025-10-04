package com.duchastel.simon.brainiac.core.process

/**
 * Interface to abstract access to an LLM model
 */
interface ModelProvider {
    /**
     * Processes the given [input] as the context window and returns the resultant
     * context window, which will include the input at the beginning or null if there
     * was an error processing.
     */
    fun process(input: String): String?
}
