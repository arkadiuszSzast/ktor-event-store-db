package com.szastarek.acl.authority

import com.szastarek.acl.Feature
import kotlinx.serialization.Serializable

sealed interface Authority

data class FeatureAccessAuthority(val feature: Feature) : Authority

@Serializable
data class EntityAccessAuthority<T : AclResource>(
    val aclResourceIdentifier: AclResourceIdentifier,
    val scopes: List<AuthorityScope<T>>
) : Authority

object AllFeaturesAuthority : Authority
object ViewAllEntitiesAuthority : Authority
object CreateAllEntitiesAuthority : Authority
object UpdateAllEntitiesAuthority : Authority
object DeleteAllEntitiesAuthority : Authority
object ManageAllEntitiesAuthority : Authority

fun List<Authority>.hasFeatureAuthority(feature: Feature): Boolean {
    return this.filterIsInstance<FeatureAccessAuthority>().any { authority -> authority.feature == feature }
}

fun <T: AclResource> List<Authority>.filterEntityAccessAuthorities(): List<EntityAccessAuthority<T>> =
    this.filterIsInstance<EntityAccessAuthority<T>>()

fun List<Authority>.hasReadAllEntitiesAuthority(): Boolean =
    any { authority -> authority is ViewAllEntitiesAuthority || authority is ManageAllEntitiesAuthority }

fun List<Authority>.hasCreateAllEntitiesAuthority(): Boolean =
    any { authority -> authority is CreateAllEntitiesAuthority || authority is ManageAllEntitiesAuthority }

fun List<Authority>.hasUpdateAllEntitiesAuthority(): Boolean =
    any { authority -> authority is UpdateAllEntitiesAuthority || authority is ManageAllEntitiesAuthority }

fun List<Authority>.hasDeleteAllEntitiesAuthority(): Boolean =
    any { authority -> authority is DeleteAllEntitiesAuthority || authority is ManageAllEntitiesAuthority }

fun List<Authority>.mergeWith(overridingAuthorities: List<Authority>): List<Authority> {
    val otherEntityAccessAuthoritiesIdentifiers = overridingAuthorities.filterIsInstance<EntityAccessAuthority<*>>()
        .map { it.aclResourceIdentifier }

    return (this.filter {
        !(it is EntityAccessAuthority<*> && it.aclResourceIdentifier in otherEntityAccessAuthoritiesIdentifiers)
    } + overridingAuthorities).distinct()
}