package com.szastarek.acl.authority

import com.szastarek.acl.Feature

interface IAuthorizedAccountAbilityProvider {
    suspend fun hasAccessTo(feature: Feature): Decision

    suspend fun <T : AclResource> canCreate(aclResource: T): Decision

    suspend fun <T : AclResource> canView(aclResource: T): Decision

    suspend fun <T : AclResource> canUpdate(aclResource: T): Decision

    suspend fun <T : AclResource> canDelete(aclResource: T): Decision

    suspend fun <T : AclResource> filterCanView(entities: Collection<T>): Collection<T>

    suspend fun ensuring(): AuthorizedAccountAbilityEnsureProvider
}