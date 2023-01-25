package com.szastarek.acl

import com.szastarek.acl.authority.Authority

sealed interface Role

data class RegularRole(val name: String, val authorities: List<Authority>)

object SuperUserRole : Role