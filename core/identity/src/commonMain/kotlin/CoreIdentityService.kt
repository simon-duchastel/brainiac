package com.brainiac.core.identity

import com.brainiac.core.identity.CoreIdentity

interface CoreIdentityService {
    fun getCoreIdentity(): CoreIdentity
    
    fun getCoreIdentityContent(): String
}