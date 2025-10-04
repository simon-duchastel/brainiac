package com.brainiac.core.fileaccess

import okio.Path

/**
 * Multiplatform file lock interface
 */
interface FileLock {
    fun release()
    fun isValid(): Boolean
}

/**
 * File system operations for transparent, hierarchical memory storage.
 */
interface FileSystemService {
    fun read(path: Path): String
    fun write(path: Path, content: String)
    fun readLtmFile(path: Path): LTMFile
    fun writeLtmFile(path: Path, ltmFile: LTMFile)
    fun readStm(): String
    fun writeStm(stm: ShortTermMemory)
    fun acquireLock(path: Path): FileLock
    fun releaseLock(path: Path)
}
