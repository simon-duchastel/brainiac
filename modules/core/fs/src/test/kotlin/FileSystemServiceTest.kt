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
    
    "should read empty string when STM file does not exist" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val stmContent = service.readStm()
        
        stmContent shouldBe ""
        
        Files.deleteIfExists(tempDir)
    }
    
    "should read STM content as raw string" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val expectedContent = """# Short-Term Memory Scratchpad

## Summary
Recent conversation about programming concepts. User is learning Kotlin and building a memory system.

---
## Structured Data
This section contains discrete, machine-readable data for immediate use.

### Goals
- [ ] Learn Kotlin programming
- [ ] Build memory system
- Complete documentation
"""
        
        service.write(stmPath, expectedContent)
        val stmContent = service.readStm()
        
        stmContent shouldBe expectedContent
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
    
    "should write structured STM and read as raw string" {
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
        val readContent = service.readStm()
        
        // Verify it contains expected sections but as raw string
        readContent.contains("# Short-Term Memory Scratchpad") shouldBe true
        readContent.contains("## Summary") shouldBe true
        readContent.contains("Test summary with multiple lines.") shouldBe true
        readContent.contains("## Structured Data") shouldBe true
        readContent.contains("Goal 1") shouldBe true
        readContent.contains("Goal 2") shouldBe true
        readContent.contains("## Event Log") shouldBe true
        readContent.contains("Test user input") shouldBe true
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
    
    "should handle any file content as raw string" {
        val tempDir = Files.createTempDirectory("test-stm")
        val stmPath = tempDir.resolve("short_term.md")
        val service = FileSystemService(stmPath)
        
        val anyContent = """# Random Markdown
        
This is not a proper STM file.

### Random timestamp with no event data
Some random content.

**User:** Incomplete event
"""
        
        service.write(stmPath, anyContent)
        val stmContent = service.readStm()
        
        // Should return the exact content as written
        stmContent shouldBe anyContent
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
    
    "should handle empty STM write and read" {
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
        val readContent = service.readStm()
        
        // Should contain the basic structure even when empty
        readContent.contains("# Short-Term Memory Scratchpad") shouldBe true
        readContent.contains("## Summary") shouldBe true
        readContent.contains("## Structured Data") shouldBe true
        readContent.contains("## Event Log") shouldBe true
        
        Files.deleteIfExists(stmPath)
        Files.deleteIfExists(tempDir)
    }
})