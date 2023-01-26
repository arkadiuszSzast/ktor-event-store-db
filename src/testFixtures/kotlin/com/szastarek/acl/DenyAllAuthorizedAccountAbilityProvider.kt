package com.szastarek.acl

import com.szastarek.acl.authority.AclResource
import com.szastarek.acl.authority.AuthorityCheckException
import com.szastarek.acl.authority.AuthorizedAccountAbilityProvider
import com.szastarek.acl.authority.DefaultAuthorizedAccountAbilityEnsureProvider
import com.szastarek.acl.authority.Deny

class DenyAllAuthorizedAccountAbilityProvider : AuthorizedAccountAbilityProvider {
    override suspend fun hasAccessTo(feature: Feature) = Deny(AuthorityCheckException("Deny all"))

    override suspend fun <T : AclResource> canCreate(aclResource: T) = Deny(AuthorityCheckException("Deny all"))

    override suspend fun <T : AclResource> canView(aclResource: T) = Deny(AuthorityCheckException("Deny all"))

    override suspend fun <T : AclResource> canUpdate(aclResource: T) = Deny(AuthorityCheckException("Deny all"))

    override suspend fun <T : AclResource> canDelete(aclResource: T) = Deny(AuthorityCheckException("Deny all"))

    override suspend fun <T : AclResource> filterCanView(entities: Collection<T>) = emptyList<T>()

    override suspend fun ensuring() = DefaultAuthorizedAccountAbilityEnsureProvider(this)
}
