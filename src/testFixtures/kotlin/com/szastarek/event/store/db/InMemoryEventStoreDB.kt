package com.szastarek.event.store.db

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.DeleteResult
import com.eventstore.dbclient.DeleteStreamOptions
import com.eventstore.dbclient.Direction
import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.ExpectedRevision
import com.eventstore.dbclient.PersistentSubscription
import com.eventstore.dbclient.Position
import com.eventstore.dbclient.ReadResult
import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.SubscribeToStreamOptions
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.WriteResult
import com.szastarek.event.store.db.support.getSystemMetadata
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.jvm.isAccessible
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job

class InMemoryEventStoreDB : EventStoreDB {

    private var streams: ConcurrentHashMap<StreamName, List<EventData>> = ConcurrentHashMap()

    private val parent: CompletableJob = Job()
    override val coroutineContext: CoroutineContext
        get() = parent

    fun clear() {
        streams.clear()
    }

    fun getStreams(): Map<StreamName, List<EventData>> {
        return streams.toMap()
    }

    override suspend fun appendToStream(
        streamName: StreamName,
        eventData: EventData,
        options: AppendToStreamOptions
    ): WriteResult {
        val value = streams[streamName].orEmpty()
        val updatedValue = value + eventData
        streams[streamName] = updatedValue
        return WriteResult::class.constructors.first().apply { isAccessible = true }
            .call(
                ExpectedRevision.expectedRevision(updatedValue.size + 1L),
                Position(updatedValue.size.toLong(), updatedValue.size.toLong())
            )
    }

    override suspend fun deleteStream(streamName: StreamName, options: DeleteStreamOptions.() -> Unit): DeleteResult {
        return deleteStream(streamName)
    }

    override suspend fun deleteStream(streamName: StreamName): DeleteResult {
        streams.remove(streamName)
        return DeleteResult::class.constructors.first().apply { isAccessible = true }
            .call(Position(0, 0))
    }

    override suspend fun readStream(
        streamName: StreamName,
        maxCount: Int,
        direction: Direction,
        resolveLinksTo: Boolean
    ): ReadResult {
        val events = streams[streamName].orEmpty().take(maxCount).apply {
            if (direction == Direction.Backwards) {
                reversed()
            }
        }.mapIndexed { index, eventData ->
            val recordedEvent = RecordedEvent::class.constructors.first()
                .apply { isAccessible = true }
                .call(
                    streamName.value,
                    index.toLong(),
                    eventData.eventId,
                    Position(index.toLong(), index.toLong()),
                    eventData.getSystemMetadata(),
                    eventData.eventData,
                    eventData.userMetadata,
                )
            ResolvedEvent(recordedEvent, recordedEvent, Position(index.toLong(), index.toLong()))
        }
        return ReadResult::class.constructors.first()
            .apply { isAccessible = true }
            .call(events, 0, events.size.toLong(), Position(events.size.toLong(), events.size.toLong()))
    }

    override suspend fun readAll(maxCount: Int, direction: Direction, resolveLinksTo: Boolean): ReadResult {
        val events = streams.flatMap { (key, value) ->
            val sortedValues = if (direction == Direction.Forwards) {
                value
            } else {
                value.reversed()
            }

            sortedValues.take(maxCount).mapIndexed { index, eventData ->
                val recordedEvent = RecordedEvent::class.constructors.first()
                    .apply { isAccessible = true }
                    .call(
                        key.value,
                        index.toLong(),
                        eventData.eventId,
                        Position(index.toLong(), index.toLong()),
                        eventData.getSystemMetadata(),
                        eventData.eventData,
                        eventData.userMetadata,
                    )
                ResolvedEvent(recordedEvent, recordedEvent, Position(index.toLong(), index.toLong()))
            }
        }

        return ReadResult::class.constructors.first()
            .apply { isAccessible = true }
            .call(events, 0, events.size.toLong(), Position(events.size.toLong(), events.size.toLong()))
    }


    override suspend fun readAllByEventType(
        eventType: EventType,
        maxCount: Int,
        direction: Direction
    ): ReadResult {
        val events = streams.flatMap { (key, value) ->
            val sortedValues = if (direction == Direction.Forwards) {
                value
            } else {
                value.reversed()
            }

            sortedValues.filter { it.eventType == eventType.value }.mapIndexed { index, eventData ->
                RecordedEvent::class.constructors.first()
                    .apply { isAccessible = true }
                    .call(
                        key.value,
                        index.toLong(),
                        eventData.eventId,
                        Position(index.toLong(), index.toLong()),
                        eventData.getSystemMetadata(),
                        eventData.eventData,
                        eventData.userMetadata,
                    )
            }
        }.take(maxCount).mapIndexed { index, event ->
            ResolvedEvent(event, event, Position(index.toLong(), index.toLong()))
        }

        return ReadResult::class.constructors.first()
            .apply { isAccessible = true }
            .call(events, 0, events.size.toLong(), Position(events.size.toLong(), events.size.toLong()))
    }

    override suspend fun readAllByEventCategory(
        eventCategory: EventCategory,
        maxCount: Int,
        direction: Direction
    ): ReadResult {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeByCorrelationId(id: UUID, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeToStream(streamName: StreamName, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun readByCorrelationId(id: UUID, maxCount: Int, direction: Direction): ReadResult {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeToStream(
        streamName: StreamName,
        options: SubscribeToStreamOptions,
        listener: EventListener
    ): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeToPersistentStream(
        streamName: StreamName,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeToPersistentStream(
        streamName: StreamName,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeToAll(listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeToAll(options: SubscribeToAllOptions, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeByStreamNameFiltered(prefix: Prefix, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeByStreamNameFiltered(regex: Regex, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeByEventType(eventType: EventType, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeByEventCategory(eventCategory: EventCategory, listener: EventListener): Subscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribePersistentByEventType(
        eventType: EventType,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribePersistentByEventType(
        eventType: EventType,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribePersistentByEventCategory(
        eventCategory: EventCategory,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription {
        TODO("Not yet implemented")
    }

    override suspend fun subscribePersistentByEventCategory(
        eventCategory: EventCategory,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription {
        TODO("Not yet implemented")
    }
}