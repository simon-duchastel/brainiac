# Context Ordering in Brainiac AI Memory System

## Overview

The Brainiac AI Memory System follows a strict context ordering principle: **least → most likely to change**. This ensures optimal LLM performance, token efficiency, and cognitive alignment with human memory patterns.

## Context Ordering Hierarchy

The Working Memory context is assembled in the following order:

### 1. Core Identity (Least Volatile)
- **Source**: `system/core_identity.md`
- **Volatility**: Never changes during operation
- **Purpose**: Foundational principles, goals, and personality
- **Rationale**: Provides stable context foundation that should never be truncated

### 2. Short-Term Memory (Medium Volatility)
- **Source**: `memory/short_term.md`
- **Volatility**: Changes frequently with user interactions
- **Components**:
  - Summary (updated during promotion cycles)
  - Structured Data (goals, facts, tasks)
  - Event Log (recent interactions)
- **Rationale**: Recent context that provides immediate situational awareness

### 3. Relevant Long-Term Memory (Low-Medium Volatility)
- **Source**: Retrieved LTM files based on search queries
- **Volatility**: Changes based on user prompt relevance
- **Purpose**: Consolidated knowledge relevant to current interaction
- **Rationale**: Provides depth and historical context, but specific to current query

### 4. User Prompt (Most Volatile)
- **Source**: Current user input
- **Volatility**: Changes every interaction
- **Purpose**: Immediate request or query
- **Rationale**: Most recent and specific context, should be closest to generation point

## Benefits of This Ordering

### LLM Effectiveness
- **Stable Foundation**: Core identity provides consistent behavioral context
- **Progressive Specificity**: Context becomes more specific toward the prompt
- **Attention Focus**: Most recent information (user prompt) gets optimal attention placement

### Token Efficiency
- **Safe Truncation**: If context must be truncated, it happens from the end
- **Essential Preservation**: Core identity and foundational context always preserved
- **Dynamic Adaptation**: LTM can be filtered based on available token budget

### Cognitive Alignment
- **Human-Inspired**: Mirrors how humans access memories (identity → recent context → specific recall → immediate focus)
- **Natural Flow**: Information flows from general principles to specific current needs
- **Memory Reinforcement**: Frequently accessed patterns become more prominent

## Implementation Guidelines

### For Core Processes
All processes that assemble context for LLM interaction must follow this ordering:

```kotlin
buildString {
    // 1. Core Identity (least volatile)
    appendLine("## Core Identity")
    appendLine(coreIdentity)
    
    // 2. Short-Term Memory (medium volatility)  
    appendLine("## Short-Term Memory")
    // ... STM content
    
    // 3. Long-Term Memory (low-medium volatility)
    if (ltmExcerpts.isNotEmpty()) {
        appendLine("## Relevant Long-Term Memory")
        // ... LTM content
    }
    
    // 4. User Prompt (most volatile)
    appendLine("## User Prompt")
    appendLine(userPrompt)
}
```

### Testing Context Order
Tests should verify that context sections appear in the correct order:

```kotlin
capturedWorkingMemory shouldContain "## Core Identity"
capturedWorkingMemory shouldContain "## Short-Term Memory"
capturedWorkingMemory shouldContain "## Relevant Long-Term Memory"
capturedWorkingMemory shouldContain "## User Prompt"

// Verify ordering
val coreIdentityIndex = capturedWorkingMemory.indexOf("## Core Identity")
val stmIndex = capturedWorkingMemory.indexOf("## Short-Term Memory")
val ltmIndex = capturedWorkingMemory.indexOf("## Relevant Long-Term Memory")
val promptIndex = capturedWorkingMemory.indexOf("## User Prompt")

coreIdentityIndex shouldBeLessThan stmIndex
stmIndex shouldBeLessThan ltmIndex
ltmIndex shouldBeLessThan promptIndex
```

## Examples

### CoreLoopProcess Implementation
The CoreLoopProcess follows this pattern in its `assembleWorkingMemory` method:

1. **Core Identity**: System's foundational principles
2. **Short-Term Memory**: Recent interaction context and structured data
3. **Relevant Long-Term Memory**: Retrieved memories matching current query
4. **User Prompt**: Current user request

This ensures that the LLM has stable foundational context while focusing on the immediate user need.

### Future Process Implementations
When implementing the other core processes (Reflection, Promotion, Organization), maintain this ordering principle:

- **Reflection Process**: Core Identity → Current STM → New Event → User Prompt
- **Promotion Process**: Core Identity → STM Event Log → Existing LTM → Promotion Candidates
- **Organization Process**: Core Identity → Access Patterns → Current LTM Structure → Reorganization Goals

## Token Budget Considerations

When implementing token limits:

1. **Always preserve Core Identity** (highest priority)
2. **Preserve User Prompt** (second priority - current focus)
3. **Truncate LTM first** (can be filtered/reduced)
4. **Truncate STM Event Log last** (recent context is valuable)

This ensures that essential context is maintained even under token constraints.