package com.brainiac.core.search

import com.brainiac.core.model.LTMFile

interface SearchService {
    fun searchLTM(queries: List<String>): List<LTMFile>
}