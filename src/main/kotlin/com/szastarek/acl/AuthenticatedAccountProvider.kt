package com.szastarek.acl

import com.szastarek.acl.authority.AuthoritiesProvider

interface AuthenticatedAccountProvider : AuthoritiesProvider {
    suspend fun currentPrincipal(): AccountContext
}
