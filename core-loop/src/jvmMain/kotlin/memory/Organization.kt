package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.xml.xml
import com.duchastel.simon.brainiac.core.process.context.BrainiacContext
import com.duchastel.simon.brainiac.core.process.prompt.Prompts
import com.duchastel.simon.brainiac.core.process.util.withModel
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@PublishedApi
internal val logger = LoggerFactory.getLogger("Organization")

/**
 * Analysis of memory access patterns.
 */
@Serializable
data class MemoryAnalysis(
    val frequentlyAccessed: List<String> = emptyList(),
    val frequentlyModified: List<String> = emptyList(),
    val coAccessedPairs: List<Pair<String, String>> = emptyList(),
    val unusedFiles: List<String> = emptyList(),
    val insights: String = ""
)

/**
 * Base class for refactoring operations.
 */
@Serializable
sealed class RefactoringOperation {
    /**
     * Strengthen relationship between two memory files.
     */
    @Serializable
    data class StrengthenRelation(
        val fromFile: String,
        val toFile: String,
        val relationDescription: String
    ) : RefactoringOperation()

    /**
     * Move a memory file to a new location.
     */
    @Serializable
    data class MoveMemory(
        val fromPath: String,
        val toPath: String,
        val reason: String
    ) : RefactoringOperation()

    /**
     * Archive a memory file that's no longer frequently accessed.
     */
    @Serializable
    data class ArchiveMemory(
        val filePath: String,
        val reason: String
    ) : RefactoringOperation()

    /**
     * Consolidate multiple related memories into one.
     */
    @Serializable
    data class ConsolidateMemories(
        val sourcePaths: List<String>,
        val targetPath: String,
        val consolidatedContent: String
    ) : RefactoringOperation()
}

/**
 * Container for a list of refactoring operations.
 */
@Serializable
data class RefactoringOperations(
    val operations: List<RefactoringOperation> = emptyList()
)

/**
 * Helper functions for executing refactoring operations.
 */

/**
 * Moves a memory file from one location to another.
 */
@PublishedApi
internal fun executeMoveMemory(
    repository: LongTermMemoryRepository,
    fromPath: String,
    toPath: String,
    reason: String
) {
    try {
        val content = repository.getLongTermMemory(fromPath)
        repository.writeLongTermMemory(toPath, content)

        // Delete source (by writing empty - actual deletion would require FileSystem access)
        // For now, we'll leave the old file in place
        // TODO: Add deleteMemory method to repository
        logger.info("Moved memory: {} -> {} (reason: {})", fromPath, toPath, reason)
    } catch (e: Exception) {
        logger.error("Failed to move memory {} -> {}: {}", fromPath, toPath, e.message, e)
    }
}

/**
 * Archives a memory file by moving it to the archive directory.
 */
@PublishedApi
internal fun executeArchiveMemory(
    repository: LongTermMemoryRepository,
    filePath: String,
    reason: String
) {
    try {
        val archivePath = "archive/$filePath"
        val content = repository.getLongTermMemory(filePath)
        repository.writeLongTermMemory(archivePath, content)

        // TODO: Add deleteMemory method to repository
        logger.info("Archived memory: {} -> {} (reason: {})", filePath, archivePath, reason)
    } catch (e: Exception) {
        logger.error("Failed to archive memory {}: {}", filePath, e.message, e)
    }
}

/**
 * Consolidates multiple memory files into one.
 */
@PublishedApi
internal fun executeConsolidateMemories(
    repository: LongTermMemoryRepository,
    sourcePaths: List<String>,
    targetPath: String,
    consolidatedContent: String
) {
    try {
        // Write consolidated content
        repository.writeLongTermMemory(targetPath, consolidatedContent)

        // TODO: Delete source files
        logger.info("Consolidated {} memories into {}", sourcePaths.size, targetPath)
        sourcePaths.forEach { logger.info("  - {}", it) }
    } catch (e: Exception) {
        logger.error("Failed to consolidate memories into {}: {}", targetPath, e.message, e)
    }
}

/**
 * Organization process for long-term memory.
 *
 * This is the "deep thought" cycle that analyzes memory access patterns and
 * refactors the LTM structure to optimize for usage patterns.
 *
 * Process:
 * 1. Read and parse access log
 * 2. Analyze patterns (frequently accessed, co-accessed, unused)
 * 3. Propose refactoring operations
 * 4. Execute refactorings
 * 5. Archive access log
 */
@AIAgentBuilderDslMarker
context(brainiacContext: BrainiacContext)
inline fun <reified T : Any> AIAgentSubgraphBuilderBase<*, *>.organizeLongTermMemory(
    name: String? = null,
    longTermMemoryRepository: LongTermMemoryRepository,
    accessLogRepository: AccessLogRepository,
): AIAgentSubgraphDelegate<T, T> = subgraph(
    name = name,
    toolSelectionStrategy = ToolSelectionStrategy.NONE,
) {
    val initialInputKey = createStorageKey<T>("${name}_initial_input")
    val accessLogKey = createStorageKey<AccessLog>("${name}_access_log")

    val setup by node<T, Unit>("${name}_setup") { input ->
        storage.set(initialInputKey, input)
    }

    val cleanup by node<Unit, T>("${name}_cleanup") {
        storage.getValue(initialInputKey)
    }

    val checkIfShouldRun by node<Unit, Boolean>("${name}_check_if_should_run") {
        !accessLogRepository.isEmpty()
    }

    val readAccessLog by node<Unit, AccessLog>("${name}_read_access_log") {
        val log = accessLogRepository.readAccessLog()
        storage.set(accessLogKey, log)
        log
    }

    val analyzePatterns by node<AccessLog, MemoryAnalysis>("${name}_analyze_patterns") { accessLog ->
        llm.writeSession {
            rewritePrompt {
                prompt("analyze_memory_patterns") {
                    system {
                        +Prompts.ANALYZE_MEMORY_PATTERNS

                        xml {
                            tag("access-log") {
                                tag("total-entries") { +accessLog.entries.size.toString() }

                                val byFile = accessLog.groupByFile()
                                tag("by-file") {
                                    byFile.forEach { (file, entries) ->
                                        tag("file", linkedMapOf("path" to file)) {
                                            tag("read-count") {
                                                +entries.count { it.action == AccessAction.READ }.toString()
                                            }
                                            tag("write-count") {
                                                +entries.count { it.action == AccessAction.WRITE }.toString()
                                            }
                                            tag("modify-count") {
                                                +entries.count { it.action == AccessAction.MODIFY }.toString()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    user {
                        +Prompts.ANALYZE_MEMORY_PATTERNS_USER
                    }
                }
            }

            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<MemoryAnalysis>().getOrNull()!!.structure
            }
        }
    }

    val proposeRefactorings by node<MemoryAnalysis, RefactoringOperations>("${name}_propose_refactorings") { analysis ->
        llm.writeSession {
            rewritePrompt {
                prompt("propose_refactorings") {
                    system {
                        +Prompts.PROPOSE_REFACTORING

                        xml {
                            tag("memory-analysis") {
                                tag("frequently-accessed") {
                                    analysis.frequentlyAccessed.forEach {
                                        tag("file") { +it }
                                    }
                                }
                                tag("frequently-modified") {
                                    analysis.frequentlyModified.forEach {
                                        tag("file") { +it }
                                    }
                                }
                                tag("co-accessed-pairs") {
                                    analysis.coAccessedPairs.forEach { (file1, file2) ->
                                        tag("pair") {
                                            tag("file1") { +file1 }
                                            tag("file2") { +file2 }
                                        }
                                    }
                                }
                                tag("unused") {
                                    analysis.unusedFiles.forEach {
                                        tag("file") { +it }
                                    }
                                }
                                tag("insights") { +analysis.insights }
                            }

                            tag("ltm-structure") {
                                longTermMemoryRepository.generateXmlMindMap()
                            }
                        }
                    }
                    user {
                        +Prompts.PROPOSE_REFACTORING_USER
                    }
                }
            }

            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<RefactoringOperations>().getOrNull()!!.structure
            }
        }
    }

    val executeRefactorings by node<RefactoringOperations, Unit>("${name}_execute_refactorings") { operations ->
        operations.operations.forEach { operation ->
            when (operation) {
                is RefactoringOperation.StrengthenRelation -> {
                    // TODO: Implement when _index.md support is added
                    // For now, just log the intention
                    logger.debug("Would strengthen relation: {} -> {}: {}", operation.fromFile, operation.toFile, operation.relationDescription)
                }

                is RefactoringOperation.MoveMemory -> {
                    executeMoveMemory(
                        longTermMemoryRepository,
                        operation.fromPath,
                        operation.toPath,
                        operation.reason
                    )
                }

                is RefactoringOperation.ArchiveMemory -> {
                    executeArchiveMemory(
                        longTermMemoryRepository,
                        operation.filePath,
                        operation.reason
                    )
                }

                is RefactoringOperation.ConsolidateMemories -> {
                    executeConsolidateMemories(
                        longTermMemoryRepository,
                        operation.sourcePaths,
                        operation.targetPath,
                        operation.consolidatedContent
                    )
                }
            }
        }
    }

    val archiveLog by node<Unit, Unit>("${name}_archive_log") {
        accessLogRepository.archiveAndClear()
    }

    edge(nodeStart forwardTo setup)
    edge(setup forwardTo checkIfShouldRun)
    edge(
        checkIfShouldRun forwardTo readAccessLog
                onCondition { it }
                transformed { }
    )
    edge(
        checkIfShouldRun forwardTo cleanup
                onCondition { !it }
                transformed { }
    )
    readAccessLog then analyzePatterns then proposeRefactorings then executeRefactorings then archiveLog then cleanup
    cleanup then nodeFinish
}
