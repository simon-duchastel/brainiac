You're an expert systems engineer and LLM expert. We're going to create a spec together for a memory and intelligence system using AI (LLMs).

# Context

## Goals

The ultimate goal is to create an AI system that users can interact with which stores its goals and memories persistently and refers to it, providing continuity and intelligence.

1. The LLM has a multi-tiered memory system in order to provide it long-term goals and intelligence. At the very least there's a short-term memory system and a long-term memory system that's persistent (transferrable)
2. The system is not opinionated about which LLM to use - it shouldn't be coupled and if needed the model used could be swapped out (it's an implementation detail)
3. The system isn't locked into proprietary or otherwise locked down system. The memory system leverages plain-text in a simple format such that it's easily understandable without special tools (ex. no binary formats)

## Bets

1. **No vector databases or bespoke memory tools**: LLMs can read and use file system operations more effectively than bespoke tools like vector databases
2. **Memory mirrors the human brain**: Inspiration should be taken from the human
3. **Memory is dynamic and automatic**: the user doesn't specifically instruct the LLM what to store in memory, the system decides for itself what's important. Ex. more frequent items are more important and thus get stored, relationships between concepts are automatically stored, etc.

# Task

## Spec Requirements

1. The user is able to interact with the LLM through natural language. The mechanics of communications aren't tightly coupled to the system - it could be command line, web UI, mobile app, etc. My idea is to run the system off my raspberry pi at home but in theory it could also be distributed or in the cloud, and flexible to change in the future.
2. There's working memory that stores ONLY the current information the LLM needs. This is likely its context window. Maybe the system should aggressively clear and compress the context window to keep things scoped maybe? Consider the tradeoffs of doing this and whether to do it or not.
3. There's short-term memory which stores a short amount of information. The goal of this memory is to bride working memory with the LLMs short-term goals and for storing concepts before being promoted to long-term memory.
4. There's long-term memory which stores a very large amount of information.
5. There may be multiple types of long-term memory which have different purposes. Consider the tradeoffs and what different types (if any) there should be. See the 'long-term memory' section.
6. There's some process that runs on some cadence that promotes short-term memory to working memory. Think deeply about what the cadence should be - every X tasks? Every hour? Every day? every 10M tokens?
7. There's some process within the LLM (hooks? system prompts? scaffolding around whatever runs the agent?) that stores relevant notes from working memory into short-term memory as it works.
8. Long-term memory is indexed in some way to make it easy to search through things. The LLM can look up where relevant memories might be in some way - maybe this is an index file in each directory, it might be a specific index portion of the memory, or something else. This might also be another type of long-term memory or relate to it in some way (like relational memories). See `long-term memory`.
9. Long-term memory is hierarchical. It's stored in a tiered directory structure so that as the LLM queries its memory it can easily retrieve common memories but with more effort retrieve more complex ones.
10. There's some process that runs on some cadence which organizes and prunes long-term memory. this may be related (or be the same) as the process that promote short-term memory to long-term. This process will ex. make more prominent concepts that are more important/common, less prominent concepts that are less important/common, associate concepts relationally, and otherwise make sure long-term memory is aligned to current usage. In this way the memory evolves over time through use.
11. Consider how, if at all, memory should be tagged. You might consider ex. being able to turn on or off certain parts of memory based on tags to enter certain modes (like inducing amnesia).

## Long-term Memory

Don't consider this prescriptive, rather consider this as ideas of different types of long-term memory that should be thought about deeply and considered whether (and how) it might get implemented. Types:
1. **Concrete**: specific topics that the intelligence knows about. These are facts and knowledge.
2. **Events**: events that have happened to the intelligence. Concretely this is likely conversations its had with the user and code/actions its taken. These are likely timestamped and sorted by recency, topic area, and/or participants.
3. **Relational**: Relating topics together. Ex. if an intelligence is asked to code a candy website for a small-business side-hustle, it might associate candy->side-hustle. The relations are likely linked somehow to the other long-term memory types in order to enhance them. Ex. when querying memory the intelligence might look up the relations of whatever concept or topic its looking for in order to find related items.
4. **Emotional**: this one I'm less sure about. You should think deeply about the tradeoffs. You could tag events/topics with specific emotions in order to enhance motiviations for the intelligence.

Consider other types of memory in the human brain as inspiration for how to effectively create intelligence.

## Your Task

1. Plan how to create this system. Think deeply about the 'goals'
2. Consider the various tradeoffs to accomplish the 'Spec Requirements'. Think deeply and out loud about the different ideas and their merits
3. Follow the 'bets' and make sure you align with their spirit in your plan
4. Create a detailed spec in 'spec.md'. This should be detailed enough that it can then be implemented in a concrete code implementation. Make sure the spec is truly a spec and has **NO** implementation details, rather keep this to the discretion of the implementor.
5. If you have any questions or clarifications that you want to run by me, please do so rather than committing to something you're not sure about
