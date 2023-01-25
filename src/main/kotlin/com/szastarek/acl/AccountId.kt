package com.szastarek.acl

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class AccountId(val value: String)