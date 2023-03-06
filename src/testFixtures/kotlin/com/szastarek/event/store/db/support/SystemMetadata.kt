package com.szastarek.event.store.db.support

import com.eventstore.dbclient.EventData
import java.time.Instant
import java.util.concurrent.TimeUnit

fun EventData.getSystemMetadata(): Map<String, String> {
    val currentMillis = Instant.now().toEpochMilli()
    val created = (TimeUnit.MICROSECONDS.convert(currentMillis, TimeUnit.MILLISECONDS) * 10).toString()
    val isJson = contentType == "application/json"
    return mapOf(
        "content-type" to contentType,
        "type" to eventType,
        "created" to created,
        "is-json" to isJson.toString()
    )
}