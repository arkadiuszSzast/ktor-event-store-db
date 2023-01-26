package com.szastarek.acl.utils

import com.szastarek.acl.AccountContext
import com.szastarek.acl.AccountId
import com.szastarek.acl.AuthenticatedAccountProvider
import com.szastarek.acl.BelongsToAccount
import com.szastarek.acl.Feature
import com.szastarek.acl.RegularRole
import com.szastarek.acl.SuperUserRole
import com.szastarek.acl.authority.AclResource
import com.szastarek.acl.authority.AclResourceBelongsToAccountPredicate
import com.szastarek.acl.authority.AclResourceIdentifier
import com.szastarek.acl.authority.Authority
import com.szastarek.acl.authority.authorities

object StaticAuthenticatedAccountProvider : AuthenticatedAccountProvider {
    override suspend fun currentPrincipal() = StaticRegularAccountContext

    override fun getRoleAuthorities() = regularAccountAuthorities

    override fun getCustomAuthorities() = customAuthorities

    override fun getInjectedAuthorities() = injectedAuthorities
}

object StaticSuperUserAuthenticatedAccountProvider : AuthenticatedAccountProvider {
    override suspend fun currentPrincipal() = StaticSuperUserAccountContext

    override fun getRoleAuthorities() = emptyList<Authority>()

    override fun getCustomAuthorities() = emptyList<Authority>()

    override fun getInjectedAuthorities() = emptyList<Authority>()
}

val regularAccountAccessibleFeature = Feature("regular-account-accessible-feature")
val regularAccountCustomAccessibleFeature = Feature("regular-account-custom-accessible-feature")
val regularAccountInjectedAccessibleFeature = Feature("regular-account-injected-accessible-feature")
val superUserAccountAccessibleFeature = Feature("super-user-account-accessible-feature")

data class Dog(val name: String, override val accountId: AccountId) : AclResource, BelongsToAccount {
    override val aclResourceIdentifier: AclResourceIdentifier
        get() = Companion.aclResourceIdentifier

    companion object {
        val aclResourceIdentifier = AclResourceIdentifier("Dog")
    }
}

data class Cat(val name: String, val age: Int) : AclResource {
    override val aclResourceIdentifier: AclResourceIdentifier
        get() = Companion.aclResourceIdentifier

    companion object {
        val aclResourceIdentifier = AclResourceIdentifier("Cat")
    }
}

data class Mouse(val age: Int) : AclResource {
    override val aclResourceIdentifier: AclResourceIdentifier
        get() = Companion.aclResourceIdentifier

    companion object {
        val aclResourceIdentifier = AclResourceIdentifier("Mouse")
    }
}
object StaticRegularAccountContext : AccountContext {
    override val accountId = AccountId("account-1")
    override val role = RegularRole("user", regularAccountAuthorities)
}

object StaticSuperUserAccountContext : AccountContext {
    override val accountId = AccountId("super-user-account-1")
    override val role = SuperUserRole
}

val regularAccountAuthorities = authorities {
    featureAccess(regularAccountAccessibleFeature)
    entityAccess<Dog>(Dog.aclResourceIdentifier) {
        createScope()
        updateScope(AclResourceBelongsToAccountPredicate())
    }
}

val customAuthorities = authorities {
    featureAccess(regularAccountCustomAccessibleFeature)
    entityAccess<Dog>(Dog.aclResourceIdentifier) {
        createScope()
        viewScope(AclResourceBelongsToAccountPredicate())
        updateScope(AclResourceBelongsToAccountPredicate())
        deleteScope(AclResourceBelongsToAccountPredicate())
    }
    entityAccess<Cat>(Cat.aclResourceIdentifier) {
        createScope()
    }
    entityAccess<Mouse>(Mouse.aclResourceIdentifier) {
        manageScope()
    }
}

val injectedAuthorities = authorities {
    featureAccess(regularAccountInjectedAccessibleFeature)
    deleteAllEntitiesAuthority()
    entityAccess<Cat>(Cat.aclResourceIdentifier) {
        viewScope()
        createScope()
        updateScope()
    }
}
