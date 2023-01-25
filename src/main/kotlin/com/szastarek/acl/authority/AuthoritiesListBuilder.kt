package com.szastarek.acl.authority

import com.szastarek.acl.Feature

class AuthoritiesListBuilder {
    private val authorities: MutableList<Authority> = mutableListOf()

    fun <T : AclResource> entityAccess(
        aclResourceIdentifier: AclResourceIdentifier,
        customize: EntityAccessAuthorityScopeBuilder<T>.() -> Unit
    ) {
        val builder = EntityAccessAuthorityScopeBuilder<T>(aclResourceIdentifier)
        builder.apply(customize)
        authorities.add(builder.build())
    }

    fun featureAccess(feature: Feature) {
        authorities.add(FeatureAccessAuthority(feature))
    }

    fun viewAllEntitiesAuthority() {
        authorities.add(ViewAllEntitiesAuthority)
    }

    fun createAllEntitiesAuthority() {
        authorities.add(CreateAllEntitiesAuthority)
    }

    fun updateAllEntitiesAuthority() {
        authorities.add(UpdateAllEntitiesAuthority)
    }

    fun deleteAllEntitiesAuthority() {
        authorities.add(DeleteAllEntitiesAuthority)
    }

    fun manageAllEntitiesAuthority() {
        authorities.add(ManageAllEntitiesAuthority)
    }

    fun allFeaturesAuthority() {
        authorities.add(AllFeaturesAuthority)
    }

    fun build() = authorities.toList()
}

fun authorities(customize: AuthoritiesListBuilder.() -> Unit) = AuthoritiesListBuilder().apply(customize).build()
