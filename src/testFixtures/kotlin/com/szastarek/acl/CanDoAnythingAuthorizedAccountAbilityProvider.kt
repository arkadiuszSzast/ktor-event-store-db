package com.szastarek.acl

import com.szastarek.acl.authority.AclResource
import com.szastarek.acl.authority.Allow
import com.szastarek.acl.authority.AuthorizedAccountAbilityProvider
import com.szastarek.acl.authority.DefaultAuthorizedAccountAbilityEnsureProvider

class CanDoAnythingAuthorizedAccountAbilityProvider : AuthorizedAccountAbilityProvider {
    override suspend fun hasAccessTo(feature: Feature) = Allow

    override suspend fun <T : AclResource> canCreate(aclResource: T) = Allow

    override suspend fun <T : AclResource> canView(aclResource: T) = Allow

    override suspend fun <T : AclResource> canUpdate(aclResource: T) = Allow

    override suspend fun <T : AclResource> canDelete(aclResource: T) = Allow

    override suspend fun <T : AclResource> filterCanView(entities: Collection<T>) = entities

    override suspend fun ensuring() = DefaultAuthorizedAccountAbilityEnsureProvider(this)
}
