package com.szastarek.acl.authority

import com.szastarek.acl.Feature

interface AuthorizedAccountAbilityEnsureProvider {
    suspend fun ensureHasAccessTo(feature: Feature)

    suspend fun <T : AclResource> ensureCanCreate(aclResource: T)

    suspend fun <T : AclResource> ensureCanUpdate(aclResource: T)

    suspend fun <T : AclResource> ensureCanDelete(aclResource: T)

    suspend fun <T : AclResource> ensureCanView(aclResource: T)

    suspend fun <T : AclResource> filterCanView(entities: Collection<T>): Collection<T>
}
