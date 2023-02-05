package com.szastarek.event.store.db

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull

val Application.eventStoreDb
    get() = pluginOrNull(EventStoreDB) ?: install(EventStoreDB)