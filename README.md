# Brainiac AI Memory System

An artificial intelligence system designed for persistent memory and continuous learning, implementing a human-inspired architecture with autonomous memory management.

## Overview

Brainiac creates an intelligence that evolves through its interactions, building a unique and useful internal model of its world and purpose. The system is built around three core principles:

- **File-System Centric**: Transparent, hierarchical storage in plain-text files for longevity and interoperability
- **Human-Inspired Architecture**: Distinct tiers for working, short-term, and long-term memory
- **Autonomous Intelligence**: Self-managing memory with emergent importance based on usage patterns

## Architecture

- **Working Memory**: Ephemeral context assembled for each interaction
- **Short-Term Memory**: Recent interaction staging area (`memory/short_term.md`)
- **Long-Term Memory**: Permanent, structured knowledge base (`memory/long_term/`)
- **Core Processes**: Four autonomous processes managing memory flow

Built with **Kotlin Multiplatform** for cross-platform compatibility while maintaining JVM performance.

## Implementation Status

✅ **CoreLoopProcess**: Complete synchronous user interaction with proper context ordering  
✅ **Context Documentation**: Comprehensive ordering guidelines for optimal LLM performance  
✅ **CoreIdentityService**: Extensible identity management architecture  
🔄 **Future**: Reflection, Promotion, and Organization processes  

See `spec-v1.md` for complete specification and `implementation-plan.md` for development roadmap.
