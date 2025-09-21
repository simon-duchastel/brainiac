package com.brainiac.core.identity

import com.brainiac.core.fs.FileSystemService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Paths

class CoreIdentityServiceTest : StringSpec({
    
    "should return core identity content from file system" {
        val mockFileSystemService = mockk<FileSystemService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        val expectedContent = "# Core Identity\n\nI am Brainiac, an AI assistant designed to help with memory management."
        
        every { mockFileSystemService.read(coreIdentityPath) } returns expectedContent
        
        val service = DefaultCoreIdentityService(mockFileSystemService, coreIdentityPath)
        
        val result = service.getCoreIdentityContent()
        
        result shouldBe expectedContent
        verify { mockFileSystemService.read(coreIdentityPath) }
    }
    
    "should parse and return core identity object" {
        val mockFileSystemService = mockk<FileSystemService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        val content = "# Core Identity\n\nI am Brainiac, an AI assistant."
        
        every { mockFileSystemService.read(coreIdentityPath) } returns content
        
        val service = DefaultCoreIdentityService(mockFileSystemService, coreIdentityPath)
        
        val result = service.getCoreIdentity()
        
        result.name shouldBe "Brainiac"
        result.role shouldBe "AI Memory Assistant"
        result.personality shouldBe "Helpful and knowledgeable"
        result.capabilities shouldContain "Memory management"
        result.capabilities shouldContain "Information retrieval"
        result.capabilities shouldContain "Learning"
        result.limitations shouldContain "Cannot access external systems"
        result.limitations shouldContain "Relies on provided memory"
        
        verify { mockFileSystemService.read(coreIdentityPath) }
    }
    
    "should handle empty content gracefully" {
        val mockFileSystemService = mockk<FileSystemService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        
        every { mockFileSystemService.read(coreIdentityPath) } returns ""
        
        val service = DefaultCoreIdentityService(mockFileSystemService, coreIdentityPath)
        
        val contentResult = service.getCoreIdentityContent()
        val identityResult = service.getCoreIdentity()
        
        contentResult shouldBe ""
        identityResult.name shouldBe "Brainiac" // Should still return default parsed values
        
        verify(exactly = 2) { mockFileSystemService.read(coreIdentityPath) }
    }
    
    "should read file only once per call" {
        val mockFileSystemService = mockk<FileSystemService>()
        val coreIdentityPath = Paths.get("/system/core_identity.md")
        val content = "# Core Identity\n\nTest content"
        
        every { mockFileSystemService.read(coreIdentityPath) } returns content
        
        val service = DefaultCoreIdentityService(mockFileSystemService, coreIdentityPath)
        
        service.getCoreIdentityContent()
        service.getCoreIdentity()
        
        verify(exactly = 2) { mockFileSystemService.read(coreIdentityPath) }
    }
})