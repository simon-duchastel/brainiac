package com.brainiac.core.identity

import com.brainiac.core.fileaccess.FileSystemService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldContain
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import okio.Path.Companion.toPath

class CoreIdentityServiceTest : StringSpec({
    
    "should return core identity content from file system" {
        val mockFileSystemService = mock<FileSystemService>()
        val coreIdentityPath = "/system/core_identity.md".toPath()
        val expectedContent =
            "# Core Identity\n\nI am Brainiac, an AI assistant designed to help with memory management."

        every { mockFileSystemService.read(coreIdentityPath) } returns expectedContent

        val service = DefaultCoreIdentityService(mockFileSystemService, coreIdentityPath)

        val result = service.getCoreIdentityContent()

        result shouldBe expectedContent
        verify { mockFileSystemService.read(coreIdentityPath) }
    }
})