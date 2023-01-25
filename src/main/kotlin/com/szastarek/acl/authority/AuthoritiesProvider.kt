package com.szastarek.acl.authority

interface AuthoritiesProvider {
    fun getRoleAuthorities(): List<Authority>
    fun getCustomAuthorities(): List<Authority>
    fun getInjectedAuthorities(): List<Authority>

    fun getJoinedAuthorities(): List<Authority> {
        return getRoleAuthorities()
            .mergeWith(getCustomAuthorities())
            .mergeWith(getInjectedAuthorities())
    }
}