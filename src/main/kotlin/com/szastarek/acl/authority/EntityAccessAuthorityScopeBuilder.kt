package com.szastarek.acl.authority

class EntityAccessAuthorityScopeBuilder<T : AclResource>(private val aclResourceIdentifier: AclResourceIdentifier) {
    private var scopes: MutableList<AuthorityScope<T>> = mutableListOf()

    fun viewScope(vararg predicates: AclResourcePredicate<T>) {
        scope(AuthorityLevel.View, predicates.toList())
    }

    fun createScope() {
        scope(AuthorityLevel.Create, emptyList())
    }

    fun updateScope(vararg predicates: AclResourcePredicate<T>) {
        scope(AuthorityLevel.Update, predicates.toList())
    }

    fun deleteScope(vararg predicates: AclResourcePredicate<T>) {
        scope(AuthorityLevel.Delete, predicates.toList())
    }

    fun manageScope(vararg predicates: AclResourcePredicate<T>) {
        scope(AuthorityLevel.Manage, predicates.toList())
    }


    fun scope(level: AuthorityLevel, predicates: List<AclResourcePredicate<T>>) {
        scopes.add(AuthorityScope(level, predicates))
    }

    fun build() = EntityAccessAuthority(aclResourceIdentifier, scopes)
}
