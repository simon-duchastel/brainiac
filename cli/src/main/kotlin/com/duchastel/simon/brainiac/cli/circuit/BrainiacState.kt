package com.duchastel.simon.brainiac.cli.circuit

import com.duchastel.simon.brainiac.cli.models.ChatMessage
import com.duchastel.simon.brainiac.cli.models.ToolActivity
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class BrainiacState(
    val messages: List<ChatMessage> = emptyList(),
    val thinking: String = "",
    val isThinking: Boolean = false,
    val isThinkingExpanded: Boolean = false,
    val toolActivities: List<ToolActivity> = emptyList(),
    val showToolDetails: Boolean = false,
    val isWaitingForResponse: Boolean = false,
    val loadingDots: Int = 0,
    val headerAnimationFrame: Int = 0,
    val inputBuffer: String = "",
    val eventSink: (BrainiacEvent) -> Unit = {}
) : CircuitUiState

sealed interface BrainiacEvent : CircuitUiEvent {
    data class SendMessage(val message: String) : BrainiacEvent
    data object ToggleThinking : BrainiacEvent
    data object ToggleToolDetails : BrainiacEvent
    data class AppendToInput(val char: String) : BrainiacEvent
    data object BackspaceInput : BrainiacEvent
    data object SubmitInput : BrainiacEvent
    data object ExitApp : BrainiacEvent
}
