package com.brainiac.core.search

import com.brainiac.core.fileaccess.LTMFile

interface SearchService {
    fun searchLTM(query: String): List<LTMFile>
}