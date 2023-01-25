package com.szastarek.acl

import com.szastarek.acl.authority.Authority
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class InjectedAuthorityContext(val authorities: List<Authority>) :
    AbstractCoroutineContextElement(InjectedAuthorityContext) {

    companion object Key : CoroutineContext.Key<InjectedAuthorityContext>
}
