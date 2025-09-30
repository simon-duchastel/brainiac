package com.brainiac.core.identity

import com.brainiac.core.model.CoreIdentity

interface CoreIdentityService {
    fun getCoreIdentity(): CoreIdentity
    
    fun getCoreIdentityContent(): String
}