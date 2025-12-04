package com.duchastel.simon.brainiac.cli

import com.duchastel.simon.brainiac.cli.circuit.BrainiacPresenter
import com.duchastel.simon.brainiac.cli.circuit.BrainiacScreen
import com.duchastel.simon.brainiac.cli.circuit.BrainiacState
import com.duchastel.simon.brainiac.cli.circuit.BrainiacUi
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.jakewharton.mosaic.runMosaicMain
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory
import sun.misc.Signal
import sun.misc.SignalHandler
import kotlin.system.exitProcess

// ============================================================================
// Main Entry Point
// ============================================================================

private val logger = LoggerFactory.getLogger("com.duchastel.simon.brainiac.cli.Main")

fun main(args: Array<String>) {
    val enableLogging = args.contains("--logging")
    configureLogging(enableLogging)

    // Register signal handlers for robust Ctrl-C handling
    // This ensures exit works even if the application is stuck
    try {
        Signal.handle(Signal("INT")) { _ ->
            logger.info("SIGINT received, exiting with code 130")
            exitProcess(130) // Standard exit code for SIGINT
        }
        Signal.handle(Signal("TERM")) { _ ->
            logger.info("SIGTERM received, exiting with code 143")
            exitProcess(143) // Standard exit code for SIGTERM
        }
    } catch (e: Exception) {
        // Signal handling might not work on all platforms, but don't fail
        logger.warn("Could not register signal handlers: ${e.message}")
    }

    val brainiacRootDirectory = "~/.brainiac/".toPath()
    val openRouterApiKey = System.getenv("OPEN_ROUTER_API_KEY")
        ?: error("OPEN_ROUTER_API_KEY environment variable not set")
    val tavilyApiKey = System.getenv("TAVILY_API_KEY")

    val shortTermMemoryRepository = ShortTermMemoryRepository(
        brainiacRootDirectory = brainiacRootDirectory
    )
    val longTermMemoryRepository = LongTermMemoryRepository(
        brainiacRootDirectory = brainiacRootDirectory
    )

    val circuit: Circuit =
        Circuit.Builder()
            .addPresenter<BrainiacScreen, BrainiacState> { screen, navigator, context ->
                BrainiacPresenter(
                    openRouterApiKey = openRouterApiKey,
                    tavilyApiKey = tavilyApiKey,
                    shortTermMemoryRepository = shortTermMemoryRepository,
                    longTermMemoryRepository = longTermMemoryRepository
                )
            }
            .addUi<BrainiacScreen, BrainiacState> { state, _ -> BrainiacUi(state) }
            .build()

    runMosaicMain {
        CircuitCompositionLocals(circuit) {
            CircuitContent(BrainiacScreen)
        }
    }
}
