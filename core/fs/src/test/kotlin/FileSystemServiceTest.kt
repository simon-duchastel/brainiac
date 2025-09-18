package com.braniac.core.fs

import com.braniac.core.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.engine.test.logging.debug
import java.nio.file.Files
import java.nio.file.Path
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
})