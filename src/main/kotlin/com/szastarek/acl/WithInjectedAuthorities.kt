package com.szastarek.acl

import com.szastarek.acl.authority.Authority
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

suspend fun <T> withInjectedAuthorities(authorities: List<Authority>, block: suspend () -> T): T {
    val currentInjectedAuthorities = currentCoroutineContext()[InjectedAuthorityContext]?.authorities ?: emptyList()
    val authoritiesToInject = (authorities + currentInjectedAuthorities).distinct()

    return withContext(currentCoroutineContext() + InjectedAuthorityContext(authoritiesToInject)) {
        block()
    }
}
