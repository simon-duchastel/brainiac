@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.duchastel.simon.brainiac.core.process

import com.duchastel.simon.brainiac.core.fileaccess.FileSystemService
import com.duchastel.simon.brainiac.core.search.SearchService
import com.duchastel.simon.brainiac.core.identity.CoreIdentityService
import com.duchastel.simon.brainiac.core.fileaccess.LTMFile

class DefaultCoreLoopProcess(
    private val fileSystemService: FileSystemService,
    private val searchService: SearchService,
    private val coreIdentityService: CoreIdentityService,
    private val modelProvider: ModelProvider,
) : CoreLoopProcess {

    override fun processUserPrompt(userPrompt: String): String {
        val stmContent = fileSystemService.readStm()

        val initialContext = buildInitialContext(userPrompt, stmContent)

        // Search for relevant LTM using combined context
        val searchQuery = "$userPrompt $initialContext"
        val ltmExcerpts = searchService.searchLTM(searchQuery)

        val workingMemory = assembleWorkingMemory(userPrompt, stmContent, ltmExcerpts)

        return modelProvider.process(workingMemory) ?: "Error processing prompt"
    }

    private fun buildInitialContext(userPrompt: String, stmContent: String): String {
        return buildString {
            appendLine("User Prompt: $userPrompt")
            appendLine()
            if (stmContent.isNotEmpty()) {
                appendLine("## Short-Term Memory")
                appendLine(stmContent)
                appendLine()
            }
        }
    }

    private fun assembleWorkingMemory(
        userPrompt: String,
        stmContent: String,
        ltmExcerpts: List<LTMFile>
    ): String {
        val coreIdentity = coreIdentityService.getCoreIdentityContent()

        return buildString {
            appendLine("# Working Memory")
            appendLine()
            appendLine("## Core Identity")
            appendLine(coreIdentity)
            appendLine()
            if (stmContent.isNotEmpty()) {
                appendLine("## Short-Term Memory")
                appendLine(stmContent)
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

            appendLine("## User Prompt")
            appendLine(userPrompt)
        }
    }
}
