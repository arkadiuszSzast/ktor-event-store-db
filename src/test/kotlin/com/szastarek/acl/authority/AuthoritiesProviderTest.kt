package com.szastarek.acl.authority

import com.szastarek.acl.Feature
import com.szastarek.acl.utils.Cat
import com.szastarek.acl.utils.Dog
import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

class AuthoritiesProviderTest : DescribeSpec({
    val authoritiesProvider = object : AuthoritiesProvider {
        override fun getRoleAuthorities(): List<Authority> {
            return authorities {
                featureAccess(Feature("feature1"))
                viewAllEntitiesAuthority()
                entityAccess<Dog>(Dog.aclResourceIdentifier) {
                    createScope()
                    updateScope()
                }
            }
        }

        override fun getCustomAuthorities(): List<Authority> {
            return authorities {
                featureAccess(Feature("feature1"))
                featureAccess(Feature("feature2"))
                viewAllEntitiesAuthority()
                entityAccess<Dog>(Dog.aclResourceIdentifier) {
                    createScope()
                    updateScope(AclResourceBelongsToAccountPredicate())
                }
            }
        }

        override fun getInjectedAuthorities(): List<Authority> {
            return authorities {
                featureAccess(Feature("feature3"))
                entityAccess<Cat>(Cat.aclResourceIdentifier) {
                    createScope()
                }
            }
        }
    }

    describe("AuthoritiesProvider") {
        it("should return joined authorities") {
            val expected = authorities {
                featureAccess(Feature("feature1"))
                featureAccess(Feature("feature2"))
                featureAccess(Feature("feature3"))
                viewAllEntitiesAuthority()
                entityAccess<Dog>(Dog.aclResourceIdentifier) {
                    createScope()
                    updateScope(AclResourceBelongsToAccountPredicate())
                }
                entityAccess<Cat>(Cat.aclResourceIdentifier) {
                    createScope()
                }
            }

            expectThat(authoritiesProvider.getJoinedAuthorities())
                .containsExactlyInAnyOrder(expected)
        }
    }
})
