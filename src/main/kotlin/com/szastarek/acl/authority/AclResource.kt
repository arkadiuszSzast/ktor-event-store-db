package com.szastarek.acl.authority


interface AclResource {
   val aclResourceIdentifier: AclResourceIdentifier
}

@JvmInline
value class AclResourceIdentifier(val name: String)
