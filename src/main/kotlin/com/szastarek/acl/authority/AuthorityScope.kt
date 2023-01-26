package com.szastarek.acl.authority

import com.szastarek.acl.AccountContext
import com.szastarek.acl.BelongsToAccount

data class AuthorityScope<T : AclResource>(
    val level: AuthorityLevel,
    val predicates: List<AclResourcePredicate<T>> = emptyList()
)

sealed interface AclResourcePredicate<in T: AclResource> {
    fun isSatisfiedBy(resource: T, accountContext: AccountContext): Boolean
}

class AclResourceBelongsToAccountPredicate<T> : AclResourcePredicate<T> where T: AclResource, T:  BelongsToAccount{
    override fun isSatisfiedBy(resource: T, accountContext: AccountContext): Boolean {
        return resource.accountId == accountContext.accountId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
