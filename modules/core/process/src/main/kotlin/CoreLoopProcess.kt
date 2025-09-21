package com.brainiac.core.process

import com.brainiac.core.fs.FileSystemService
import com.brainiac.core.llm.LLMService
import com.brainiac.core.search.SearchService
import com.brainiac.core.model.CoreIdentity
import com.brainiac.core.model.ShortTermMemory
import com.brainiac.core.model.LTMFile
import java.nio.file.Path

class CoreLoopProcess(
    private val fileSystemService: FileSystemService,
    private val llmService: LLMService,
    private val searchService: SearchService,
    private val coreIdentityPath: Path
) {
    
    fun processUserPrompt(userPrompt: String): String {
        val stm = fileSystemService.readStm()
        
        val initialContext = buildInitialContext(userPrompt, stm)
        
        val searchQueries = llmService.generateSearchQueries(userPrompt, initialContext)
        
        val ltmExcerpts = searchService.searchLTM(searchQueries)
        
        val workingMemory = assembleWorkingMemory(userPrompt, stm, ltmExcerpts)
        
        return llmService.generateResponse(workingMemory)
    }
    
    private fun buildInitialContext(userPrompt: String, stm: ShortTermMemory): String {
        return buildString {
            appendLine("User Prompt: $userPrompt")
            appendLine()
            appendLine("## Summary")
            appendLine(stm.summary)
            appendLine()
            appendLine("## Structured Data")
            appendLine("### Goals")
            stm.structuredData.goals.forEach { goal ->
                appendLine("- $goal")
            }
            appendLine()
            appendLine("### Key Facts & Decisions")
            stm.structuredData.keyFacts.forEach { fact ->
                appendLine("- $fact")
            }
            appendLine()
            appendLine("### Tasks")
            stm.structuredData.tasks.forEach { task ->
                appendLine("- $task")
            }
        }
    }
    
    private fun assembleWorkingMemory(
        userPrompt: String,
        stm: ShortTermMemory,
        ltmExcerpts: List<LTMFile>
    ): String {
        val coreIdentity = fileSystemService.read(coreIdentityPath)
        
        return buildString {
            appendLine("# Working Memory")
            appendLine()
            appendLine("## Core Identity")
            appendLine(coreIdentity)
            appendLine()
            appendLine("## User Prompt")
            appendLine(userPrompt)
            appendLine()
            appendLine("## Short-Term Memory")
            appendLine("### Summary")
            appendLine(stm.summary)
            appendLine()
            appendLine("### Structured Data")
            appendLine("#### Goals")
            stm.structuredData.goals.forEach { goal ->
                appendLine("- $goal")
            }
            appendLine()
            appendLine("#### Key Facts & Decisions")
            stm.structuredData.keyFacts.forEach { fact ->
                appendLine("- $fact")
            }
            appendLine()
            appendLine("#### Tasks")
            stm.structuredData.tasks.forEach { task ->
                appendLine("- $task")
            }
            appendLine()
            appendLine("#### Event Log")
            stm.eventLog.forEach { event ->
                appendLine("### ${event.timestamp}")
                appendLine("**User:** \"${event.user}\"")
                appendLine("**AI:** \"${event.ai}\"")
                appendLine("**Thoughts:** ${event.thoughts}")
                appendLine()
            }
            
            if (ltmExcerpts.isNotEmpty()) {
                appendLine("## Relevant Long-Term Memory")
                ltmExcerpts.forEachIndexed { index, ltmFile ->
                    appendLine("### Memory ${index + 1}")
                    appendLine("**UUID:** ${ltmFile.frontmatter.uuid}")
                    appendLine("**Tags:** ${ltmFile.frontmatter.tags.joinToString(", ")}")
                    appendLine("**Created:** ${ltmFile.frontmatter.createdAt}")
                    appendLine("**Updated:** ${ltmFile.frontmatter.updatedAt}")
                    appendLine("**Reinforcement Count:** ${ltmFile.frontmatter.reinforcementCount}")
                    appendLine()
                    appendLine(ltmFile.content)
                    appendLine()
                }
            }
        }
    }
}