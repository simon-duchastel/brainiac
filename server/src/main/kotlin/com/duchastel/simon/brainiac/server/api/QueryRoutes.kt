package com.duchastel.simon.brainiac.server.api

import com.duchastel.simon.brainiac.server.model.CreateQueryRequest
import com.duchastel.simon.brainiac.server.model.CreateQueryResponse
import com.duchastel.simon.brainiac.server.model.QueryEvent
import com.duchastel.simon.brainiac.server.service.AgentRunner
import com.duchastel.simon.brainiac.server.service.QueryExecutionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Configures the query-related routes.
 */
fun Route.queryRoutes(
    queryExecutionService: QueryExecutionService,
    agentRunner: AgentRunner
) {
    route("/queries") {
        /**
         * POST /queries - Create a new query
         */
        post {
            val request = try {
                call.receive<CreateQueryRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                return@post
            }

            if (request.query.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Query cannot be blank"))
                return@post
            }

            val execution = queryExecutionService.startQuery(request.query)
            if (execution == null) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Another query is already in progress")
                )
                return@post
            }

            // Start execution asynchronously
            agentRunner.executeQuery(execution.id, execution.query)

            call.respond(
                HttpStatusCode.Created,
                CreateQueryResponse(
                    queryId = execution.id,
                    status = execution.status.name.lowercase()
                )
            )
        }

        /**
         * GET /queries/{id} - Stream query events via SSE
         */
        sse("{id}") {
            val queryId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Query ID is required"))
                return@sse
            }

            val execution = queryExecutionService.getExecution(queryId)
            if (execution == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Query not found"))
                return@sse
            }

            val eventChannel = queryExecutionService.getEventChannel()
            if (eventChannel == null) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Event stream not available")
                )
                return@sse
            }

            try {
                // Send existing events first
                execution.events.forEach { event ->
                    send(io.ktor.sse.ServerSentEvent(data = Json.encodeToString(event)))
                }

                // Stream new events
                for (event in eventChannel) {
                    send(io.ktor.sse.ServerSentEvent(data = Json.encodeToString(event)))

                    // Close stream after completion or error
                    if (event is QueryEvent.Completed || event is QueryEvent.Error) {
                        break
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed, end the stream gracefully
            }
        }
    }
}
