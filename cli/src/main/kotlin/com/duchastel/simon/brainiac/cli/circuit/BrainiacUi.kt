package com.duchastel.simon.brainiac.cli.circuit

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.components.*
import com.duchastel.simon.brainiac.cli.ui.utils.Spacer
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column

@Composable
fun BrainiacUi(state: BrainiacState) {
    Column(
        modifier = Modifier.onKeyEvent { keyEvent ->
            when (keyEvent) {
                KeyEvent("Ctrl-T"), KeyEvent("Ctrl-t") -> {
                    state.eventSink(BrainiacEvent.ToggleThinking)
                    true
                }
                KeyEvent("Ctrl-A"), KeyEvent("Ctrl-a") -> {
                    state.eventSink(BrainiacEvent.ToggleToolDetails)
                    true
                }
                KeyEvent("Ctrl-C"), KeyEvent("Ctrl-c") -> {
                    state.eventSink(BrainiacEvent.ExitApp)
                    true
                }
                KeyEvent("Enter") -> {
                    state.eventSink(BrainiacEvent.SubmitInput)
                    true
                }
                KeyEvent("Backspace") -> {
                    state.eventSink(BrainiacEvent.BackspaceInput)
                    true
                }
                else -> {
                    // Handle printable characters
                    val char = keyEvent.key
                    if (char.length == 1 && (char[0].isLetterOrDigit() || char[0].isWhitespace() || char[0] in "!@#$%^&*()_+-=[]{}|;:',.<>?/`~\"\\")) {
                        state.eventSink(BrainiacEvent.AppendToInput(char))
                        true
                    } else {
                        false
                    }
                }
            }
        }
    ) {
        HeaderPanel()
        Spacer()
        ThinkingPanel(
            thinking = state.thinking,
            isThinking = state.isThinking,
            isExpanded = state.isThinkingExpanded,
            dots = state.loadingDots
        )
        Spacer()
        ToolActivityPanel(
            activities = state.toolActivities,
            showDetails = state.showToolDetails
        )
        Spacer()
        ConversationPanel(messages = state.messages.takeLast(10))
        Spacer()
        StatusPanel(
            isWaiting = state.isWaitingForResponse,
            dots = state.loadingDots
        )
        Spacer()
        InputPanel(
            inputBuffer = state.inputBuffer,
            isDisabled = state.isWaitingForResponse
        )
        FooterPanel()
    }
}
