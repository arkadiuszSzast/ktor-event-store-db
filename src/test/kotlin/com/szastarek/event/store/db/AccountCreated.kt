package com.szastarek.event.store.db

data class AccountCreated(
    val accountId: String,
    val accountName: String
)
