package com.duchastel.simon.brainiac.server.model

import kotlinx.serialization.Serializable

/**
 * Request to create a new query.
 */
@Serializable
data class CreateQueryRequest(
    val query: String
)

/**
 * Response when a query is created.
 */
@Serializable
data class CreateQueryResponse(
    val queryId: String,
    val status: String
)

/**
 * Status of a query execution.
 */
enum class QueryStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    ERROR
}

/**
 * Types of events that can be streamed from a query execution.
 */
@Serializable
sealed class QueryEvent {
    @Serializable
    data class Started(val queryId: String, val query: String) : QueryEvent()

    @Serializable
    data class AgentMessage(val content: String) : QueryEvent()

    @Serializable
    data class ToolCall(val tool: String, val arguments: String) : QueryEvent()

    @Serializable
    data class Completed(val result: String) : QueryEvent()

    @Serializable
    data class Error(val message: String) : QueryEvent()
}

/**
 * Represents a query execution with its current state.
 */
data class QueryExecution(
    val id: String,
    val query: String,
    var status: QueryStatus,
    val events: MutableList<QueryEvent> = mutableListOf(),
    var result: String? = null,
    var error: String? = null
)
