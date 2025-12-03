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

// ============================================================================
// Main Entry Point
// ============================================================================

fun main(args: Array<String>) {
    val enableLogging = args.contains("--logging")
    configureLogging(enableLogging)

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
