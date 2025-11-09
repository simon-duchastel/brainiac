# Pull Request: Add talk tool to separate AI thinking from communication

## Summary

This PR implements a paradigm shift in how Brainiac handles AI communication. Instead of showing all AI responses to users, the AI's inference becomes internal "thinking" and an explicit `talk` tool is required for user-facing communication.

### Key Changes

1. **New TalkTool** (`tools/src/jvmMain/kotlin/com/duchastel/simon/brainiac/tools/talk/TalkTool.kt`)
   - SimpleTool that takes a `message` argument
   - AI must call this tool to communicate with users
   - Clear description emphasizing this is the ONLY way to talk to users

2. **Updated AgentEvent** (`core-loop/src/jvmMain/kotlin/callbacks/AgentEvent.kt`)
   - Added `Thought` event type for internal AI reasoning
   - Added `Talk` event type for user-facing communication
   - Maintains backward compatibility with existing events

3. **Updated ToolUse** (`core-loop/src/jvmMain/kotlin/callbacks/ToolUse.kt`)
   - Added `Talk(message: String)` variant
   - Tracks talk tool usage alongside memory operations

4. **Modified Main.kt** (`cli/src/jvmMain/kotlin/Main.kt`)
   - **Critical change**: Removed printing of `Message.Assistant` content (now internal thoughts)
   - Added detection of `talk` tool calls in event handler
   - Extracts and prints message argument from talk tool
   - Maintains existing memory tool status messages

5. **Planning Document** (`planning.md`)
   - Comprehensive documentation of design decisions
   - Tradeoff analysis and architectural considerations
   - Implementation details and learnings

## Behavior Change

**Before:**
- AI `Message.Assistant` responses were directly shown to users
- All AI output was visible

**After:**
- AI `Message.Assistant` responses are hidden (internal thinking)
- Only `talk` tool calls are shown to users
- Clean separation between thinking and communication

## Benefits

- **Cleaner output**: Users only see intended communication, not reasoning process
- **Internal reasoning space**: AI can think through problems without cluttering interface
- **Explicit communication**: Clear distinction between thinking and talking
- **Follows existing patterns**: Consistent with BashTool, WebSearchTool implementation

## Testing

- Implementation follows established tool patterns
- Event handler logic verified through code review
- Ready for integration testing in full environment

## Design Decisions

See `planning.md` for detailed analysis of:
- Alternative approaches considered
- Tradeoffs evaluated
- Token usage implications
- Future enhancement possibilities

## Files Changed

- `tools/src/jvmMain/kotlin/com/duchastel/simon/brainiac/tools/talk/TalkTool.kt` (new)
- `core-loop/src/jvmMain/kotlin/callbacks/AgentEvent.kt` (modified)
- `core-loop/src/jvmMain/kotlin/callbacks/ToolUse.kt` (modified)
- `cli/src/jvmMain/kotlin/Main.kt` (modified)
- `planning.md` (new)

## Branch

`claude/add-talk-tool-thinking-011CUxrK5xGTXth2NxkxKFQn`

## Instructions

To create the PR on GitHub:
1. Visit: https://github.com/simon-duchastel/brainiac/pull/new/claude/add-talk-tool-thinking-011CUxrK5xGTXth2NxkxKFQn
2. Use the content of this file for the PR description
3. Title: "feat: Add talk tool to separate AI thinking from communication"
