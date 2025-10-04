@file:OptIn(ExperimentalTime::class)

package com.duchastel.simon.brainiac.core.fileaccess

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import okio.FileSystem
import okio.SYSTEM
import kotlin.random.Random

class FileSystemServiceTest : StringSpec({

    val fs = FileSystem.SYSTEM

    "should read empty string when STM file does not exist" {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test-stm-${Random.nextLong()}"
        fs.createDirectories(tempDir)
        val stmPath = tempDir / "short_term.md"
        val service = DefaultFileSystemService(stmPath, fs)

        val stmContent = service.readStm()

        stmContent shouldBe ""

        fs.deleteRecursively(tempDir)
    }

    "should read STM content" {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test-stm-${Random.nextLong()}"
        fs.createDirectories(tempDir)
        val stmPath = tempDir / "short_term.md"
        val service = DefaultFileSystemService(stmPath, fs)

        val expectedContent = """# Short-Term Memory

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

        fs.deleteRecursively(tempDir)
    }
    
    "should write STM content" {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test-stm-${Random.nextLong()}"
        fs.createDirectories(tempDir)
        val stmPath = tempDir / "short_term.md"
        val service = DefaultFileSystemService(stmPath, fs)

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
        readContent.contains("# Short-Term Memory") shouldBe true
        readContent.contains("## Summary") shouldBe true
        readContent.contains("Test summary with multiple lines.") shouldBe true
        readContent.contains("## Structured Data") shouldBe true
        readContent.contains("Goal 1") shouldBe true
        readContent.contains("Goal 2") shouldBe true
        readContent.contains("## Event Log") shouldBe true
        readContent.contains("Test user input") shouldBe true

        fs.deleteRecursively(tempDir)
    }
})
