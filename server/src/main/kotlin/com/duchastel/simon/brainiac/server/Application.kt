package com.duchastel.simon.brainiac.server

import com.duchastel.simon.brainiac.server.api.queryRoutes
import com.duchastel.simon.brainiac.server.service.AgentRunner
import com.duchastel.simon.brainiac.server.service.AgentRunnerConfig
import com.duchastel.simon.brainiac.server.service.QueryExecutionService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.serialization.json.Json

/**
 * Main entry point for the Brainiac server.
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

/**
 * Configures the Ktor application.
 */
fun Application.module() {
    // Load configuration from environment variables
    val googleApiKey = environment.config.propertyOrNull("brainiac.googleApiKey")?.getString()
        ?: System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")

    val openRouterApiKey = environment.config.propertyOrNull("brainiac.openRouterApiKey")?.getString()
        ?: System.getenv("OPEN_ROUTER_API_KEY")
        ?: error("OPEN_ROUTER_API_KEY environment variable not set")

    val tavilyApiKey = environment.config.propertyOrNull("brainiac.tavilyApiKey")?.getString()
        ?: System.getenv("TAVILY_API_KEY")

    val brainiacRootDirectory = environment.config.propertyOrNull("brainiac.rootDirectory")?.getString()
        ?: System.getenv("BRAINIAC_ROOT_DIR")
        ?: "~/.brainiac/"

    // Initialize services
    val queryExecutionService = QueryExecutionService()
    val agentRunner = AgentRunner(
        config = AgentRunnerConfig(
            googleApiKey = googleApiKey,
            openRouterApiKey = openRouterApiKey,
            tavilyApiKey = tavilyApiKey,
            brainiacRootDirectory = brainiacRootDirectory
        ),
        queryExecutionService = queryExecutionService
    )

    // Configure plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(SSE)

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    // Configure routing
    routing {
        get("/") {
            call.respond(
                mapOf(
                    "service" to "Brainiac Server",
                    "version" to "1.0.0",
                    "endpoints" to listOf(
                        "POST /queries - Create a new query",
                        "GET /queries/{id} - Stream query events (SSE)"
                    )
                )
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }

        queryRoutes(queryExecutionService, agentRunner)
    }
}
