package com.szastarek.acl.authority

import com.szastarek.acl.AccountId
import com.szastarek.acl.CanDoAnythingAuthorizedAccountAbilityProvider
import com.szastarek.acl.DenyAllAuthorizedAccountAbilityProvider
import com.szastarek.acl.Feature
import com.szastarek.acl.utils.Dog
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isSuccess

class DefaultAuthorizedAccountAbilityEnsureProviderTest : DescribeSpec({

    val canDoAnythingAbilityEnsureProvider =
        DefaultAuthorizedAccountAbilityEnsureProvider(CanDoAnythingAuthorizedAccountAbilityProvider())
    val denyAllAbilityEnsureProvider =
        DefaultAuthorizedAccountAbilityEnsureProvider(DenyAllAuthorizedAccountAbilityProvider())

    describe("DefaultAuthorizedAccountAbilityEnsureProvider") {

        it("should not throw when action is allowed") {
            expect {
                catching { canDoAnythingAbilityEnsureProvider.ensureCanCreate(Dog("Burek", AccountId("some-account"))) }
                    .isSuccess()

                catching { canDoAnythingAbilityEnsureProvider.ensureCanUpdate(Dog("Burek", AccountId("some-account"))) }
                    .isSuccess()

                catching { canDoAnythingAbilityEnsureProvider.ensureCanDelete(Dog("Burek", AccountId("some-account"))) }
                    .isSuccess()

                catching { canDoAnythingAbilityEnsureProvider.ensureCanView(Dog("Burek", AccountId("some-account"))) }
                    .isSuccess()

                catching { canDoAnythingAbilityEnsureProvider.ensureHasAccessTo(Feature("some-feature")) }
                    .isSuccess()

                canDoAnythingAbilityEnsureProvider.filterCanView(listOf(Dog("Burek", AccountId("some-account"))))
                    .shouldContainAll(Dog("Burek", AccountId("some-account")))
            }
        }

        it("should throw when action is not allowed") {
            expect {
                catching { denyAllAbilityEnsureProvider.ensureCanCreate(Dog("Burek", AccountId("some-account"))) }
                    .isFailure()
                    .isA<AuthorityCheckException>()

                catching { denyAllAbilityEnsureProvider.ensureCanUpdate(Dog("Burek", AccountId("some-account"))) }
                    .isFailure()
                    .isA<AuthorityCheckException>()

                catching { denyAllAbilityEnsureProvider.ensureCanDelete(Dog("Burek", AccountId("some-account"))) }
                    .isFailure()
                    .isA<AuthorityCheckException>()

                catching { denyAllAbilityEnsureProvider.ensureCanView(Dog("Burek", AccountId("some-account"))) }
                    .isFailure()
                    .isA<AuthorityCheckException>()

                catching { denyAllAbilityEnsureProvider.ensureHasAccessTo(Feature("some-feature")) }
                    .isFailure()
                    .isA<AuthorityCheckException>()

                denyAllAbilityEnsureProvider.filterCanView(listOf(Dog("Burek", AccountId("some-account"))))
                    .shouldBeEmpty()
            }
        }
    }
})
