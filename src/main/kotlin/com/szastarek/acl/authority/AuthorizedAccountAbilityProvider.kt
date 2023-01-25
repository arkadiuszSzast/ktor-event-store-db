package com.szastarek.acl.authority

import com.szastarek.acl.AuthenticatedAccountProvider
import com.szastarek.acl.Feature
import mu.KotlinLogging

class AuthorizedAccountAbilityProvider(
    private val authenticatedAccountProvider: AuthenticatedAccountProvider
) : IAuthorizedAccountAbilityProvider {
    private val logger = KotlinLogging.logger {}

    override suspend fun hasAccessTo(feature: Feature): Decision {
        val hasAccessToFeature = authenticatedAccountProvider.getJoinedAuthorities()
            .hasFeatureAuthority(feature)

        if (!hasAccessToFeature) {
            val message = "Account with id: [${authenticatedAccountProvider.currentPrincipal().accountId}] " +
                    "has no access to ${feature.name} feature."
            logger.warn { message }
            return Deny(AuthorityCheckException(message))
        }
        return Allow
    }

    override suspend fun <T : AclResource> canCreate(aclResource: T): Decision {
        val accountContext = authenticatedAccountProvider.currentPrincipal()
        val canCreate = authenticatedAccountProvider.getJoinedAuthorities()
            .filterEntityAccessAuthorities<T>()
            .find { it.aclResourceIdentifier == aclResource.aclResourceIdentifier }
            ?.scopes
            ?.filter { it.level == AuthorityLevel.Create || it.level == AuthorityLevel.Manage }
            ?.any { scope -> scope.predicates.all { it.isSatisfiedBy(aclResource, accountContext) } } ?: false

        if (!canCreate) {
            val message = refusalMessage(AuthorityLevel.Create, aclResource)
            logger.warn { message }
            return Deny(AuthorityCheckException(message))
        }
        return Allow
    }

    override suspend fun <T : AclResource> canView(aclResource: T): Decision {
        val accountContext = authenticatedAccountProvider.currentPrincipal()
        val canView = authenticatedAccountProvider.getJoinedAuthorities()
            .filterEntityAccessAuthorities<T>()
            .find { it.aclResourceIdentifier == aclResource.aclResourceIdentifier }
            ?.scopes
            ?.filter { it.level == AuthorityLevel.View || it.level == AuthorityLevel.Manage }
            ?.any { scope -> scope.predicates.all { it.isSatisfiedBy(aclResource, accountContext) } } ?: false

        if (!canView) {
            val message = refusalMessage(AuthorityLevel.View, aclResource)
            logger.warn { message }
            return Deny(AuthorityCheckException(message))
        }
        return Allow
    }

    override suspend fun <T : AclResource> canUpdate(aclResource: T): Decision {
        val accountContext = authenticatedAccountProvider.currentPrincipal()
        val canUpdate = authenticatedAccountProvider.getJoinedAuthorities()
            .filterEntityAccessAuthorities<T>()
            .find { it.aclResourceIdentifier == aclResource.aclResourceIdentifier }
            ?.scopes
            ?.filter { it.level == AuthorityLevel.Update || it.level == AuthorityLevel.Manage }
            ?.any { scope -> scope.predicates.all { it.isSatisfiedBy(aclResource, accountContext) } } ?: false

        if (!canUpdate) {
            val message = refusalMessage(AuthorityLevel.Update, aclResource)
            logger.warn { message }
            return Deny(AuthorityCheckException(message))
        }
        return Allow
    }

    override suspend fun <T : AclResource> canDelete(aclResource: T): Decision {
        val accountContext = authenticatedAccountProvider.currentPrincipal()
        val canDelete = authenticatedAccountProvider.getJoinedAuthorities()
            .filterEntityAccessAuthorities<T>()
            .find { it.aclResourceIdentifier == aclResource.aclResourceIdentifier }
            ?.scopes
            ?.filter { it.level == AuthorityLevel.Update || it.level == AuthorityLevel.Manage }
            ?.any { scope -> scope.predicates.all { it.isSatisfiedBy(aclResource, accountContext) } } ?: false

        if (!canDelete) {
            val message = refusalMessage(AuthorityLevel.Delete, aclResource)
            logger.warn { message }
            return Deny(AuthorityCheckException(message))
        }
        return Allow
    }

    override suspend fun <T : AclResource> filterCanView(entities: Collection<T>): Collection<T> {
        return entities.filter { canView(it).toBoolean() }
    }

    private suspend fun refusalMessage(level: AuthorityLevel, aclResource: AclResource) =
        "Account with id: [${authenticatedAccountProvider.currentPrincipal().accountId}] cannot perform $level action " +
                "on ${aclResource.aclResourceIdentifier} resource."

    override suspend fun ensuring() = AuthorizedAccountAbilityEnsureProvider(this)
}
