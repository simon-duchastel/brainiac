package com.duchastel.simon.brainiac.core.agent

/**
 * Interface to abstract access to an AI agent
 */
interface Agent {
    /**
     * Processes the given [input] as the context window and returns the resultant
     * context window, which will include the input at the beginning or null if there
     * was an error processing.
     */
    suspend fun process(input: String): String?
}
