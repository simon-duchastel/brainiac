package com.braniac.core.fs

import com.braniac.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant

class FileSystemServiceTest {
    
    private val fileSystemService = FileSystemService()
    
    @Test
    fun `should read and write simple text files`(@TempDir tempDir: Path) {
        val testFile = tempDir.resolve("test.txt")
        val content = "Hello, World!"
        
        fileSystemService.write(testFile, content)
        val readContent = fileSystemService.read(testFile)
        
        assertEquals(content, readContent)
    }
    
    @Test
    fun `should read and write basic LTM files`(@TempDir tempDir: Path) {
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
        
        assertEquals(content, readContent)
    }
    
    @Test
    fun `should acquire and release file locks`(@TempDir tempDir: Path) {
        val testFile = tempDir.resolve("locked.txt")
        
        val lock = fileSystemService.acquireLock(testFile)
        assertNotNull(lock)
        
        assertThrows(IllegalStateException::class.java) {
            fileSystemService.acquireLock(testFile)
        }
        
        fileSystemService.releaseLock(testFile)
        
        // Should be able to acquire again after release
        val lock2 = fileSystemService.acquireLock(testFile)
        assertNotNull(lock2)
        fileSystemService.releaseLock(testFile)
    }
}