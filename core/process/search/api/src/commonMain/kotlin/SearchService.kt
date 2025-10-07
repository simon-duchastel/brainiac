package com.duchastel.simon.brainiac.core.search

import com.duchastel.simon.brainiac.core.fileaccess.LTMFile

interface SearchService {
    suspend fun searchLTM(query: String): List<LTMFile>
}
