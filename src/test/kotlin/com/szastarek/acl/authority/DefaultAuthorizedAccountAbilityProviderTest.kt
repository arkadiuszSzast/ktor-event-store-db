package com.szastarek.acl.authority

import com.szastarek.acl.AccountId
import com.szastarek.acl.utils.Cat
import com.szastarek.acl.utils.Dog
import com.szastarek.acl.utils.Mouse
import com.szastarek.acl.utils.StaticAuthenticatedAccountProvider
import com.szastarek.acl.utils.StaticRegularAccountContext
import com.szastarek.acl.utils.StaticSuperUserAuthenticatedAccountProvider
import com.szastarek.acl.utils.regularAccountAccessibleFeature
import com.szastarek.acl.utils.regularAccountCustomAccessibleFeature
import com.szastarek.acl.utils.regularAccountInjectedAccessibleFeature
import com.szastarek.acl.utils.superUserAccountAccessibleFeature
import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class DefaultAuthorizedAccountAbilityProviderTest : DescribeSpec({

    val authorizedAccountAbilityProvider = DefaultAuthorizedAccountAbilityProvider(StaticAuthenticatedAccountProvider)
    val authorizedSuperUserAbilityProvider = DefaultAuthorizedAccountAbilityProvider(StaticSuperUserAuthenticatedAccountProvider)

    describe("DefaultAuthorizedAccountAbilityProvider") {

        describe("regular account") {

            it("should allow accessing feature") {
                expectThat(authorizedAccountAbilityProvider.hasAccessTo(regularAccountAccessibleFeature)) {
                    isEqualTo(Allow)
                }
            }

            it("should allow accessing custom feature") {
                expectThat(authorizedAccountAbilityProvider.hasAccessTo(regularAccountCustomAccessibleFeature)) {
                    isEqualTo(Allow)
                }
            }

            it("should allow accessing injected feature") {
                expectThat(authorizedAccountAbilityProvider.hasAccessTo(regularAccountInjectedAccessibleFeature)) {
                    isEqualTo(Allow)
                }
            }

            it("should disallow accessing super user feature") {
                expectThat(authorizedAccountAbilityProvider.hasAccessTo(superUserAccountAccessibleFeature)) {
                    isA<Deny>()
                }
            }

            it("should allow deleting all entities") {
                expectThat(authorizedAccountAbilityProvider.canDelete(Dog("Burek", AccountId("some-account")))) {
                    isA<Allow>()
                }
                expectThat(authorizedAccountAbilityProvider.canDelete(Cat("Bella", 5))) {
                    isA<Allow>()
                }
                expectThat(authorizedAccountAbilityProvider.canDelete(Mouse(2))) {
                    isA<Allow>()
                }
            }

            it("should allow updating own dog entity") {
                expectThat(
                    authorizedAccountAbilityProvider.canUpdate(
                        Dog("Burek", StaticRegularAccountContext.accountId)
                    )
                ) {
                    isEqualTo(Allow)
                }
            }

            it("should disallow updating  dog entity that belongs to other account") {
                expectThat(authorizedAccountAbilityProvider.canUpdate(Dog("Burek", AccountId("some-account")))) {
                    isA<Deny>()
                }
            }

            it("should allow creating cat entity") {
                expectThat(authorizedAccountAbilityProvider.canCreate(Cat("Bella", 5))) {
                    isEqualTo(Allow)
                }
            }

            it("should allow updating cat entity") {
                expectThat(authorizedAccountAbilityProvider.canUpdate(Cat("Bella", 5))) {
                    isEqualTo(Allow)
                }
            }

            it("should allow any action on mouse entity") {
                expectThat(authorizedAccountAbilityProvider.canCreate(Mouse(5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedAccountAbilityProvider.canUpdate(Mouse(5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedAccountAbilityProvider.canDelete(Mouse(5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedAccountAbilityProvider.canView(Mouse(5))) {
                    isEqualTo(Allow)
                }
            }

            it("should filter entities that can be viewed") {
                val entities = listOf(
                    Dog("Burek", StaticRegularAccountContext.accountId),
                    Dog("Burek", AccountId("some-account")),
                    Cat("Bella", 5),
                    Mouse(2)
                )
                expectThat(authorizedAccountAbilityProvider.filterCanView(entities)) {
                    isEqualTo(
                        listOf(
                            Dog("Burek", StaticRegularAccountContext.accountId), Cat("Bella", 5), Mouse(2)
                        )
                    )
                }
            }
        }

        describe("super user account") {

            it("should allow accessing all features") {
                expectThat(authorizedSuperUserAbilityProvider.hasAccessTo(regularAccountAccessibleFeature)) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.hasAccessTo(regularAccountCustomAccessibleFeature)) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.hasAccessTo(regularAccountInjectedAccessibleFeature)) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.hasAccessTo(superUserAccountAccessibleFeature)) {
                    isEqualTo(Allow)
                }
            }

            it("should allow viewing all entities") {
                expectThat(authorizedSuperUserAbilityProvider.canView(Dog("Burek", AccountId("some-account")))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canView(Cat("Bella", 5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canView(Mouse(2))) {
                    isEqualTo(Allow)
                }
            }

            it("should allow updating all entities") {
                expectThat(authorizedSuperUserAbilityProvider.canUpdate(Dog("Burek", AccountId("some-account")))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canUpdate(Cat("Bella", 5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canUpdate(Mouse(2))) {
                    isEqualTo(Allow)
                }
            }

            it("should allow creating all entities") {
                expectThat(authorizedSuperUserAbilityProvider.canCreate(Dog("Burek", AccountId("some-account")))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canCreate(Cat("Bella", 5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canCreate(Mouse(2))) {
                    isEqualTo(Allow)
                }
            }

            it("should allow deleting all entities") {
                expectThat(authorizedSuperUserAbilityProvider.canDelete(Dog("Burek", AccountId("some-account")))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canDelete(Cat("Bella", 5))) {
                    isEqualTo(Allow)
                }
                expectThat(authorizedSuperUserAbilityProvider.canDelete(Mouse(2))) {
                    isEqualTo(Allow)
                }
            }

            it("should return all entities when filtering") {
                val entities = listOf(
                    Dog("Burek", StaticRegularAccountContext.accountId),
                    Dog("Burek", AccountId("some-account")),
                    Cat("Bella", 5),
                    Mouse(2)
                )
                expectThat(authorizedSuperUserAbilityProvider.filterCanView(entities)) {
                    isEqualTo(entities)
                }
            }
        }
    }
})
