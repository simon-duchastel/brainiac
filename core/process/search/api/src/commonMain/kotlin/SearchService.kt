package com.duchastel.simon.brainiac.core.search

import com.duchastel.simon.brainiac.core.fileaccess.LTMFile

interface SearchService {
    fun searchLTM(query: String): List<LTMFile>
}
