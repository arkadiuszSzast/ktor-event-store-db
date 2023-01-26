package com.szastarek.acl

import com.szastarek.acl.authority.FeatureAccessAuthority
import com.szastarek.acl.authority.authorities
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.currentCoroutineContext
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class WithInjectedAuthoritiesKtTest : DescribeSpec({

    describe("WithInjectedAuthorities") {

        it("should use injected authorities") {
            withInjectedAuthorities(authorities { featureAccess(Feature("some-feature")) }) {
                expectThat(currentCoroutineContext()[InjectedAuthorityContext]?.authorities) {
                    isNotNull()
                        .containsExactlyInAnyOrder(FeatureAccessAuthority(Feature("some-feature")))
                }
            }
        }

        it("should be able to use injected authorities in nested blocks") {
            withInjectedAuthorities(authorities { featureAccess(Feature("some-feature")) }) {
                withInjectedAuthorities(authorities { featureAccess(Feature("some-other-feature")) }) {
                    expectThat(currentCoroutineContext()[InjectedAuthorityContext]?.authorities) {
                        isNotNull()
                            .containsExactlyInAnyOrder(
                                FeatureAccessAuthority(Feature("some-feature")),
                                FeatureAccessAuthority(Feature("some-other-feature")))
                    }
                }
            }
        }

        it("same injected authorities should be distinct") {
            withInjectedAuthorities(authorities { featureAccess(Feature("some-feature")) }) {
                withInjectedAuthorities(authorities { featureAccess(Feature("some-feature")) }) {
                    expectThat(currentCoroutineContext()[InjectedAuthorityContext]?.authorities) {
                        isNotNull()
                            .hasSize(1)
                            .containsExactlyInAnyOrder(FeatureAccessAuthority(Feature("some-feature")))
                    }
                }
            }
        }

        it("when authorities are not injected then context should be null") {
            expectThat(currentCoroutineContext()[InjectedAuthorityContext]?.authorities).isNull()
        }
    }
})
