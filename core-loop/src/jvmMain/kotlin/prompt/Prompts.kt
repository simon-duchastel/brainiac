package com.duchastel.simon.brainiac.core.process.prompt

import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.text.TextContentBuilderBase
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemory

object Prompts {
    const val BRAINIAC_SYSTEM = """You are Brainiac, an AI assistant with advanced memory capabilities.
You have access to both short-term and long-term memory systems.

Your goal is to provide helpful, accurate responses while maintaining
and organizing your memory system."""

    const val IDENTIFY_EVENTS = """Review the conversation and the current short-term memory.
Identify any events that should be added to the events list based on the current context.

Return all new events to be added (not including current events), in structured format."""

    const val UPDATE_GOALS = """Review the conversation and the current short-term memory.
Identify any goals that should be added, removed, or have their completion status changed.

Return the updated complete list of goals, including all goals which should be kept, in structured format."""

    const val UPDATE_THOUGHTS = """Review the conversation and the current short-term memory.
Identify any thoughts that should be distilled and added to the thoughts list

Return all new thoughts to be added (not including current thoughts), in structured format."""

    const val RECALL_LONG_TERM_MEMORY_INSTRUCTION = """Given the following user request and a mind map of available long-term memory files,
identify which memory files would be helpful to retrieve, if any.

Return a list of file paths (relative to the long-term-memory directory) in structured format.
If no files would be helpful, return an empty list.

Don't return memories just for the sake of it - only return memories that would be genuinely helpful. Be thorough yet selective!"""

    const val IDENTIFY_MEMORY_PROMOTIONS = """Please analyze the short-term memory below and identify important information
that should be saved to long-term memory.

Return a list of memory promotions with filenames and content.
If no memories need to be saved, return an empty list.

Don't save memories just for the sake of it - only save memories that would be genuinely helpful for long-term storage. Consider memories which are either:
1. useful pieces of information, or
2. episodic (events which happened that were interesting or notable for summarization and storage)"""

    const val CLEAN_SHORT_TERM_MEMORY = """The following short-term memory has been analyzed, and important information
has been promoted to long-term memory.

Return a cleaned version of the short-term memory that:
1. Removes information that was promoted to long-term memory
2. Removes any unneeded or redundant information
3. Retains only recent, actionable context that is still relevant"""

    const val ANALYZE_MEMORY_PATTERNS = """You are analyzing access patterns for a long-term memory system.

Given the access log data below, identify:
1. Frequently accessed memories (files with high READ count)
2. Frequently modified memories (files with high WRITE/MODIFY count)
3. Co-accessed memories (files that tend to be accessed close together in time)
4. Unused memories (files that haven't been accessed recently or at all)

Provide insights about what these patterns reveal about the system's information needs.

Return a structured MemoryAnalysis with:
- frequentlyAccessed: List of file paths with high access counts
- frequentlyModified: List of file paths with high modification counts
- coAccessedPairs: List of pairs of file paths that are accessed together
- unusedFiles: List of file paths that are rarely or never accessed
- insights: String with your analysis of what these patterns mean"""

    const val PROPOSE_REFACTORING = """You are optimizing a long-term memory system based on access patterns.

Given the memory analysis and current LTM structure below, propose refactoring operations to improve organization.

Consider:
- Strengthening relationships between co-accessed concepts (add cross-references)
- Moving frequently accessed memories to more accessible locations
- Archiving memories that haven't been accessed (move to archive/)
- Consolidating redundant or closely related memories into single files

Be conservative - only propose changes with clear benefits. Avoid unnecessary churn.

Return a RefactoringOperations structure with a list of specific operations to perform.

Available operation types:
- StrengthenRelation: Add a relationship link between two files
- MoveMemory: Move a file to a new location in the hierarchy
- ArchiveMemory: Move a rarely-used file to the archive
- ConsolidateMemories: Merge multiple related files into one"""

    fun TextContentBuilderBase<*>.summarizeWorkingMemory(
        updatedShortTermMemory: ShortTermMemory,
    ) = markdown {
        +"Summarize the current context based on the updated short-term memory below:"

        updatedShortTermMemory.asXmlRepresentation()

        +"Remove any irrelevant or redundant information as well as any information already included in the updated short-term memory"
        +"For example, discard any information pertaining to completed goals which you feel will not be relevant moving forward"
        +"Keep any information that will be useful in the future for working memory, ie. information immediately pertinent to what the user is asking you to do."
    }
}
