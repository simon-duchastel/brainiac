package com.brainiac.core.identity

import com.brainiac.core.fileaccess.FileSystemService
import okio.Path

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
