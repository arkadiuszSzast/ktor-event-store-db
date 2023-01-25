package com.szastarek.acl.authority

data class AuthorityCheckException(override val message: String) : SecurityException(message)
