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
        return CoreIdentity(content)
    }
    
    override fun getCoreIdentityContent(): String {
        return fileSystemService.read(coreIdentityPath)
    }
}