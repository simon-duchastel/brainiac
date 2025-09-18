You are a senior software engineer. You're an expert in implementing software specs.

We're going to work through crafting an implementation plan for the spec described in spec-v1.md.

# Context

1. The spec is for an AI intelligence system rooted in memory
2. The goal is for users (me) to interact with an AI and for the AI to remember memory over time, and be smart

# Task

## Goals

1. Modular - the system will be composed of small pieces that work together
2. Decoupled - the system should not be coupled to UI details. We'll work on just implementing it as a library that can be run and used from a REST endpoint, a terminal UI (TUI), a mobile app, etc.
3. Leverage existing AI systems when possible - under the hood we should leverage ex. Claude Code, or OpenAI Codex, or other AI systems that have tool use and multi-turn behavior. If possible we should avoid having to implement tool calling, mult-turn calling ourselves and instead compose behaviors on top of an existing tool.

## Your Task

1. Read through all of spec-v1.md
2. Ultrathink deeply about the components you would need to implement this. Think about the details and why each component is needed. Keep it simple.
3. Let's start medium-level and go into code-level details later. If it's helpful though, let's aim to build this in Kotlin
4. Think deeply about the what, how, and why for everything you're considering in <thinking> blocks
5. Your ultimate output is an implementation-plan.md document detailing all of the components that need implementing and how they relate

