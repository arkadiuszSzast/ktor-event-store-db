package com.szastarek.acl

import com.szastarek.acl.authority.Authority
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class AuthenticatedAccountContext(val accountContext: AccountContext, val authorities: List<Authority>) :
    AbstractCoroutineContextElement(AuthenticatedAccountContext) {

    companion object Key : CoroutineContext.Key<AuthenticatedAccountContext>
}
