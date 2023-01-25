package com.szastarek.acl.authority

import java.util.UUID

sealed interface Decision {
    fun toBoolean(): Boolean

}

inline fun Decision.onDeny(block: () -> Unit) = when (this) {
    is Deny -> block()
    is Allow -> Unit
}

object Allow : Decision {
    override fun toBoolean() = true
}

data class Deny(val reason: Throwable) : Decision {
    override fun toBoolean() = false
}
