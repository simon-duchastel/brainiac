package com.brainiac.core.identity

interface CoreIdentityService {
    fun getCoreIdentity(): CoreIdentity

    fun getCoreIdentityContent(): String
}
