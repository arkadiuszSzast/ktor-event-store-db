package com.szastarek.event.store.db

import com.eventstore.dbclient.EventDataBuilder
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kotest.core.spec.style.DescribeSpec
import java.util.UUID
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class InMemoryEventStoreDBTest : DescribeSpec({
    val eventStoreDB = InMemoryEventStoreDB()
    val json = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())

    beforeEach {
        eventStoreDB.clear()
    }

    describe("InMemoryEventStoreDB") {

        it("can append to stream") {
            //given
            val event = AccountCreated("1", "test")
            val eventAsBytes = json.writeValueAsBytes(event)
            val eventData = EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventAsBytes)
                .metadataAsBytes("metadata".toByteArray())
                .build()

            //arrange
            eventStoreDB.appendToStream(StreamName("test-stream"), eventData)
        }

        it("can delete stream") {
            //given
            val event = AccountCreated("1", "test")
            val eventAsBytes = json.writeValueAsBytes(event)
            val eventData = EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventAsBytes)
                .metadataAsBytes("metadata".toByteArray())
                .build()
            eventStoreDB.appendToStream(StreamName("test-stream"), eventData)

            //when
            eventStoreDB.deleteStream(StreamName("test-stream"))

            expectThat(eventStoreDB.getStreams()[StreamName("test-stream")]).isNull()
        }

        it("can read from stream") {
            //given
            val event = AccountCreated("1", "test")
            val eventAsBytes = json.writeValueAsBytes(event)
            val eventData = EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventAsBytes)
                .metadataAsBytes("metadata".toByteArray())
                .build()
            eventStoreDB.appendToStream(StreamName("test-stream"), eventData)

            //arrange
            val result = eventStoreDB.readStream(StreamName("test-stream"))
            val events = result.events
            expectThat(events) {
                hasSize(1)
            }
        }

        it("can read all events") {
            val event = AccountCreated("1", "test")
            val eventOnDifferentStream = AccountCreated("1", "test")
            val eventAsBytes = json.writeValueAsBytes(event)
            val eventOnDifferentStreamAsBytes = json.writeValueAsBytes(eventOnDifferentStream)
            val eventData = EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventAsBytes)
                .metadataAsBytes("metadata".toByteArray())
                .build()
            val eventOnDifferentStreamEventData =
                EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventOnDifferentStreamAsBytes)
                    .metadataAsBytes("metadata".toByteArray())
                    .build()
            eventStoreDB.appendToStream(StreamName("test-stream"), eventData)
            eventStoreDB.appendToStream(StreamName("test-2-stream"), eventOnDifferentStreamEventData)

            //arrange
            val result = eventStoreDB.readAll()
            val events = result.events
            expectThat(events) {
                hasSize(2)
                all { get { this.event.eventType }.isEqualTo("AccountCreated") }
            }
        }

        it("can read events by type") {
            //given
            val event = AccountCreated("1", "test")
            val eventOnDifferentStream = AccountCreated("1", "test")
            val differentTypeEvent = AccountCreated("2", "test-2")
            val eventAsBytes = json.writeValueAsBytes(event)
            val eventOnDifferentStreamAsBytes = json.writeValueAsBytes(eventOnDifferentStream)
            val differentTypeEventAsBytes = json.writeValueAsBytes(differentTypeEvent)
            val eventData = EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventAsBytes)
                .metadataAsBytes("metadata".toByteArray())
                .build()
            val eventOnDifferentStreamEventData =
                EventDataBuilder.json(UUID.randomUUID(), "AccountCreated", eventOnDifferentStreamAsBytes)
                    .metadataAsBytes("metadata".toByteArray())
                    .build()
            val differentTypeEventData =
                EventDataBuilder.json(UUID.randomUUID(), "DifferentType", differentTypeEventAsBytes)
                    .metadataAsBytes("metadata".toByteArray())
                    .build()
            eventStoreDB.appendToStream(StreamName("test-stream"), eventData)
            eventStoreDB.appendToStream(StreamName("test-2-stream"), eventOnDifferentStreamEventData)
            eventStoreDB.appendToStream(StreamName("test-stream"), differentTypeEventData)

            //arrange
            val result = eventStoreDB.readAllByEventType(EventType("AccountCreated"))
            val events = result.events
            expectThat(events) {
                hasSize(2)
                all { get { this.event.eventType }.isEqualTo("AccountCreated") }
            }
        }
    }
})