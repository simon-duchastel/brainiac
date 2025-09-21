package com.brainiac.core.fs

import com.brainiac.core.model.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

class FileSystemServiceTest : StringSpec({
    
    val fileSystemService = FileSystemService()
    
    "should read and write simple text files" {
        val tempDir = Files.createTempDirectory("test")
        val testFile = tempDir.resolve("test.txt")
        val content = "Hello, World!"
        
        fileSystemService.write(testFile, content)
        val readContent = fileSystemService.read(testFile)
        
        readContent shouldBe content
        
        Files.deleteIfExists(testFile)
        Files.deleteIfExists(tempDir)
    }
    
    "should read and write basic LTM files" {
        val tempDir = Files.createTempDirectory("test")
        val testFile = tempDir.resolve("test.md")
        val content = """---
uuid: test-123
createdAt: 2023-01-01T00:00:00Z
updatedAt: 2023-01-02T00:00:00Z
tags:
- test
- memory
reinforcementCount: 1
---
# Test Content

This is a test."""
        
        fileSystemService.write(testFile, content)
        val readContent = fileSystemService.read(testFile)
        
        readContent shouldBe content
        
        Files.deleteIfExists(testFile)
        Files.deleteIfExists(tempDir)
    }
    
    "should acquire and release file locks" {
        val tempDir = Files.createTempDirectory("test")
        val testFile = tempDir.resolve("locked.txt")
        
        val lock = fileSystemService.acquireLock(testFile)
        lock shouldNotBe null
        
        shouldThrow<IllegalStateException> {
            fileSystemService.acquireLock(testFile)
        }
        
        fileSystemService.releaseLock(testFile)
        
        // Should be able to acquire again after release
        val lock2 = fileSystemService.acquireLock(testFile)
        lock2 shouldNotBe null
        fileSystemService.releaseLock(testFile)
        
        Files.deleteIfExists(testFile)
        Files.deleteIfExists(tempDir)
    }
    
    "should read empty STM when file does not exist" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val stm = service.readStm()
        
        stm.summary shouldBe ""
        stm.structuredData.goals shouldHaveSize 0
        stm.structuredData.keyFacts shouldHaveSize 0
        stm.structuredData.tasks shouldHaveSize 0
        stm.eventLog shouldHaveSize 0
        
        Files.deleteIfExists(tempDir)
    }
    
    "should parse valid STM markdown correctly" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val stmContent = """# Short-Term Memory Scratchpad

## Summary
Recent conversation about programming concepts. User is learning Kotlin and building a memory system.

---
## Structured Data
This section contains discrete, machine-readable data for immediate use.

### Goals
- [ ] Learn Kotlin programming
- [ ] Build memory system
- Complete documentation

### Key Facts & Decisions
- User prefers functional programming
- System uses file-based storage
- Markdown format chosen

### Tasks
- [ ] Implement STM functionality
- [ ] Write comprehensive tests
- Review code quality

---
## Event Log
A reverse-chronological log of recent interactions. New events are appended to the top.

### 2023-01-01T15:30:00Z
**User:** "How do I implement STM?"
**AI:** "To implement STM, you need to parse markdown..."
**Thoughts:** User asking about implementation details

### 2023-01-01T15:00:00Z
**User:** "What is STM?"
**AI:** "Short-Term Memory is a staging area..."
**Thoughts:** Basic question about architecture
"""
        
        service.write(stmPath, stmContent)
        val stm = service.readStm()
        
        stm.summary shouldBe "Recent conversation about programming concepts. User is learning Kotlin and building a memory system."
        
        stm.structuredData.goals shouldHaveSize 3
        stm.structuredData.goals[0] shouldBe "Learn Kotlin programming"
        stm.structuredData.goals[1] shouldBe "Build memory system"
        stm.structuredData.goals[2] shouldBe "Complete documentation"
        
        stm.structuredData.keyFacts shouldHaveSize 3
        stm.structuredData.keyFacts[0] shouldBe "User prefers functional programming"
        stm.structuredData.keyFacts[1] shouldBe "System uses file-based storage"
        stm.structuredData.keyFacts[2] shouldBe "Markdown format chosen"
        
        stm.structuredData.tasks shouldHaveSize 3
        stm.structuredData.tasks[0] shouldBe "Implement STM functionality"
        stm.structuredData.tasks[1] shouldBe "Write comprehensive tests"
        stm.structuredData.tasks[2] shouldBe "Review code quality"
        
        stm.eventLog shouldHaveSize 2
        stm.eventLog[0].timestamp shouldBe Instant.parse("2023-01-01T15:30:00Z")
        stm.eventLog[0].user shouldBe "How do I implement STM?"
        stm.eventLog[0].ai shouldBe "To implement STM, you need to parse markdown..."
        stm.eventLog[0].thoughts shouldBe "User asking about implementation details"
        
        stm.eventLog[1].timestamp shouldBe Instant.parse("2023-01-01T15:00:00Z")
        stm.eventLog[1].user shouldBe "What is STM?"
        stm.eventLog[1].ai shouldBe "Short-Term Memory is a staging area..."
        stm.eventLog[1].thoughts shouldBe "Basic question about architecture"
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
    
    "should write and read STM roundtrip correctly" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val originalStm = ShortTermMemory(
            summary = "Test summary with multiple lines.\nSecond line of summary.",
            structuredData = StructuredData(
                goals = listOf("Goal 1", "Goal 2"),
                keyFacts = listOf("Fact 1", "Fact 2"),
                tasks = listOf("Task 1", "Task 2")
            ),
            eventLog = listOf(
                Event(
                    timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    user = "Test user input",
                    ai = "Test AI response",
                    thoughts = "Test thoughts"
                ),
                Event(
                    timestamp = Instant.parse("2023-01-01T11:00:00Z"),
                    user = "Earlier input",
                    ai = "Earlier response",
                    thoughts = ""
                )
            )
        )
        
        service.writeStm(originalStm)
        val readStm = service.readStm()
        
        readStm.summary shouldBe originalStm.summary
        readStm.structuredData.goals shouldBe originalStm.structuredData.goals
        readStm.structuredData.keyFacts shouldBe originalStm.structuredData.keyFacts
        readStm.structuredData.tasks shouldBe originalStm.structuredData.tasks
        readStm.eventLog shouldHaveSize originalStm.eventLog.size
        
        readStm.eventLog[0].timestamp shouldBe originalStm.eventLog[0].timestamp
        readStm.eventLog[0].user shouldBe originalStm.eventLog[0].user
        readStm.eventLog[0].ai shouldBe originalStm.eventLog[0].ai
        readStm.eventLog[0].thoughts shouldBe originalStm.eventLog[0].thoughts
        
        readStm.eventLog[1].timestamp shouldBe originalStm.eventLog[1].timestamp
        readStm.eventLog[1].user shouldBe originalStm.eventLog[1].user
        readStm.eventLog[1].ai shouldBe originalStm.eventLog[1].ai
        readStm.eventLog[1].thoughts shouldBe originalStm.eventLog[1].thoughts
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
    
    "should handle malformed STM markdown gracefully" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val malformedContent = """# Random Markdown
        
This is not a proper STM file.

### Random timestamp with no event data
Some random content.

**User:** Incomplete event
"""
        
        service.write(stmPath, malformedContent)
        val stm = service.readStm()
        
        // Should return empty STM on malformed content
        stm.summary shouldBe ""
        stm.structuredData.goals shouldHaveSize 0
        stm.structuredData.keyFacts shouldHaveSize 0
        stm.structuredData.tasks shouldHaveSize 0
        stm.eventLog shouldHaveSize 0
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
    
    "should handle empty sections correctly" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val emptyStm = ShortTermMemory(
            summary = "",
            structuredData = StructuredData(
                goals = emptyList(),
                keyFacts = emptyList(),
                tasks = emptyList()
            ),
            eventLog = emptyList()
        )
        
        service.writeStm(emptyStm)
        val readStm = service.readStm()
        
        readStm.summary shouldBe ""
        readStm.structuredData.goals shouldHaveSize 0
        readStm.structuredData.keyFacts shouldHaveSize 0
        readStm.structuredData.tasks shouldHaveSize 0
        readStm.eventLog shouldHaveSize 0
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
})