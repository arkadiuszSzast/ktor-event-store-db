package com.szastarek.acl.authority

import kotlinx.serialization.Serializable

interface AclResource {
   val aclResourceIdentifier: AclResourceIdentifier
}

@JvmInline
@Serializable
value class AclResourceIdentifier(val name: String)
