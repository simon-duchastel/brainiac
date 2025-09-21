package com.brainiac.core.identity

import com.brainiac.core.fs.FileSystemService
import com.brainiac.core.model.CoreIdentity
import java.nio.file.Path

class DefaultCoreIdentityService(
    private val fileSystemService: FileSystemService,
    private val coreIdentityPath: Path
) : CoreIdentityService {
    
    override fun getCoreIdentity(): CoreIdentity {
        val content = fileSystemService.read(coreIdentityPath)
        return parseCoreIdentityContent(content)
    }
    
    override fun getCoreIdentityContent(): String {
        return fileSystemService.read(coreIdentityPath)
    }
    
    private fun parseCoreIdentityContent(content: String): CoreIdentity {
        return CoreIdentity(
            name = "Brainiac",
            role = "AI Memory Assistant",
            personality = "Helpful and knowledgeable",
            capabilities = listOf("Memory management", "Information retrieval", "Learning"),
            limitations = listOf("Cannot access external systems", "Relies on provided memory")
        )
    }
}