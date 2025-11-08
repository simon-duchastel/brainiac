package com.duchastel.simon.brainiac.server.service

import com.duchastel.simon.brainiac.server.model.QueryEvent
import com.duchastel.simon.brainiac.server.model.QueryExecution
import com.duchastel.simon.brainiac.server.model.QueryStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Service that manages query execution lifecycle and enforces single-query concurrency.
 */
class QueryExecutionService {
    private val mutex = Mutex()
    private var currentExecution: QueryExecution? = null
    private var eventChannel: Channel<QueryEvent>? = null

    /**
     * Attempts to start a new query execution.
     *
     * @param query The query string to execute
     * @return The query execution if started successfully, null if another query is already running
     */
    suspend fun startQuery(query: String): QueryExecution? = mutex.withLock {
        if (currentExecution != null && currentExecution!!.status == QueryStatus.RUNNING) {
            return null // Another query is already in progress
        }

        val queryId = UUID.randomUUID().toString()
        val execution = QueryExecution(
            id = queryId,
            query = query,
            status = QueryStatus.RUNNING
        )

        currentExecution = execution
        eventChannel = Channel(Channel.UNLIMITED)

        return execution
    }

    /**
     * Gets the current query execution.
     *
     * @param queryId The ID of the query to retrieve
     * @return The query execution if found and matches the ID, null otherwise
     */
    suspend fun getExecution(queryId: String): QueryExecution? = mutex.withLock {
        if (currentExecution?.id == queryId) currentExecution else null
    }

    /**
     * Gets the event channel for the current query execution.
     */
    fun getEventChannel(): Channel<QueryEvent>? = eventChannel

    /**
     * Publishes an event to the current query's event stream.
     */
    suspend fun publishEvent(event: QueryEvent) {
        eventChannel?.send(event)
        mutex.withLock {
            currentExecution?.events?.add(event)
        }
    }

    /**
     * Marks the current query as completed with a result.
     */
    suspend fun completeQuery(result: String) = mutex.withLock {
        currentExecution?.let {
            it.status = QueryStatus.COMPLETED
            it.result = result
            publishEvent(QueryEvent.Completed(result))
            eventChannel?.close()
        }
    }

    /**
     * Marks the current query as failed with an error.
     */
    suspend fun failQuery(error: String) = mutex.withLock {
        currentExecution?.let {
            it.status = QueryStatus.ERROR
            it.error = error
            publishEvent(QueryEvent.Error(error))
            eventChannel?.close()
        }
    }

    /**
     * Clears the current execution (for testing or cleanup).
     */
    suspend fun clearExecution() = mutex.withLock {
        eventChannel?.close()
        currentExecution = null
        eventChannel = null
    }
}
