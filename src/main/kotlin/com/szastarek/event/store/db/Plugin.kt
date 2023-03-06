package com.szastarek.event.store.db

import com.eventstore.dbclient.AppendToStreamOptions
import com.eventstore.dbclient.DeleteResult
import com.eventstore.dbclient.DeleteStreamOptions
import com.eventstore.dbclient.Direction
import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import com.eventstore.dbclient.EventStoreDBConnectionString.parseOrThrow
import com.eventstore.dbclient.EventStoreDBPersistentSubscriptionsClient
import com.eventstore.dbclient.NackAction
import com.eventstore.dbclient.PersistentSubscription
import com.eventstore.dbclient.PersistentSubscriptionListener
import com.eventstore.dbclient.Position
import com.eventstore.dbclient.ReadAllOptions
import com.eventstore.dbclient.ReadResult
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.SubscribeToStreamOptions
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionFilter
import com.eventstore.dbclient.SubscriptionListener
import com.eventstore.dbclient.WriteResult
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.log
import io.ktor.util.AttributeKey
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.slf4j.Logger

typealias EventListener = suspend ResolvedEvent.() -> Unit
typealias PersistentEventListener = suspend (subscription: PersistentSubscription, event: ResolvedEvent) -> Unit
typealias SubscriptionErrorEventListener = suspend (subscription: Subscription?, throwable: Throwable) -> Unit
typealias PersistentSubscriptionErrorEventListener = suspend (subscription: PersistentSubscription?, throwable: Throwable) -> Unit

interface EventStoreDB : CoroutineScope {
    data class Configuration(
        var connectionString: String? = null,
        var eventStoreSettings: EventStoreDBClientSettings =
            EventStoreDBClientSettings.builder().buildConnectionSettings(),
        var logger: Logger,
        var subscriptionErrorListener: SubscriptionErrorEventListener = { subscription, throwable ->
            logger.error("Subscription [${subscription?.subscriptionId}] failed due to ${throwable.message}")
        },
        var persistentSubscriptionErrorEventListener: PersistentSubscriptionErrorEventListener = { subscription, throwable ->
            logger.error("Persistent subscription [${subscription?.subscriptionId}] failed due to ${throwable.message}")
        },
        var reSubscribeOnDrop: Boolean = true
    )

    suspend fun appendToStream(
        streamName: StreamName, eventData: EventData, options: AppendToStreamOptions = AppendToStreamOptions.get()
    ): WriteResult

    suspend fun readByCorrelationId(
        id: UUID, maxCount: Int= Int.MAX_VALUE, direction: Direction = Direction.Forwards
    ): ReadResult

    suspend fun readStream(
        streamName: StreamName,
        maxCount: Int= Int.MAX_VALUE,
        direction: Direction = Direction.Forwards,
        resolveLinksTo: Boolean = true
    ): ReadResult

    suspend fun readAll(
        maxCount: Int= Int.MAX_VALUE,
        direction: Direction = Direction.Forwards,
        resolveLinksTo: Boolean = true
    ): ReadResult

    suspend fun readAllByEventType(
        eventType: EventType,
        maxCount: Int= Int.MAX_VALUE,
        direction: Direction = Direction.Forwards
    ): ReadResult

    suspend fun readAllByEventCategory(
        eventCategory: EventCategory,
        maxCount: Int= Int.MAX_VALUE,
        direction: Direction = Direction.Forwards
    ): ReadResult

    suspend fun subscribeByCorrelationId(id: UUID, listener: EventListener): Subscription
    suspend fun subscribeToStream(streamName: StreamName, listener: EventListener): Subscription
    suspend fun subscribeToStream(
        streamName: StreamName,
        options: SubscribeToStreamOptions = SubscribeToStreamOptions.get(),
        listener: EventListener
    ): Subscription

    suspend fun subscribeToPersistentStream(
        streamName: StreamName, customerGroup: CustomerGroup, listener: PersistentEventListener
    ): PersistentSubscription

    suspend fun subscribeToPersistentStream(
        streamName: StreamName,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription

    suspend fun subscribeToAll(listener: EventListener): Subscription
    suspend fun subscribeToAll(options: SubscribeToAllOptions, listener: EventListener): Subscription
    suspend fun subscribeByStreamNameFiltered(prefix: Prefix, listener: EventListener): Subscription
    suspend fun subscribeByStreamNameFiltered(regex: Regex, listener: EventListener): Subscription
    suspend fun subscribeByEventType(eventType: EventType, listener: EventListener): Subscription
    suspend fun subscribeByEventCategory(eventCategory: EventCategory, listener: EventListener): Subscription
    suspend fun subscribePersistentByEventType(
        eventType: EventType,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription

    suspend fun subscribePersistentByEventCategory(
        eventCategory: EventCategory,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription

    suspend fun subscribePersistentByEventType(
        eventType: EventType,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription

    suspend fun subscribePersistentByEventCategory(
        eventCategory: EventCategory,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription

    suspend fun deleteStream(streamName: StreamName): DeleteResult
    suspend fun deleteStream(streamName: StreamName, options: DeleteStreamOptions.() -> Unit): DeleteResult

    companion object Feature : BaseApplicationPlugin<Application, Configuration, EventStoreDB> {
        override val key: AttributeKey<EventStoreDB> = AttributeKey("EventStoreDB")
        private val closedEvent = io.ktor.events.EventDefinition<Unit>()

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): EventStoreDBPlugin {
            val applicationMonitor = pipeline.environment.monitor
            val configuration = Configuration(logger = pipeline.log).apply(configure)
            val plugin = EventStoreDBPlugin(configuration)

            applicationMonitor.subscribe(ApplicationStopPreparing) {
                plugin.shutdown()
                it.monitor.raise(closedEvent, Unit)
            }
            return plugin
        }
    }
}

class EventStoreDBPlugin(private val config: EventStoreDB.Configuration) : EventStoreDB {
    private val parent: CompletableJob = Job()
    override val coroutineContext: CoroutineContext
        get() = parent

    private val streamRevisionBySubscriptionId = ConcurrentHashMap<String, Long>()
    private val positionBySubscriptionId = ConcurrentHashMap<String, Position>()

    private val client = config.connectionString
        ?.let { connectionString -> EventStoreDBClient.create(parseOrThrow(connectionString)) }
        ?: EventStoreDBClient.create(config.eventStoreSettings)

    private val persistentClient = config.connectionString
        ?.let { connectionString -> EventStoreDBPersistentSubscriptionsClient.create(parseOrThrow(connectionString)) }
        ?: EventStoreDBPersistentSubscriptionsClient.create(config.eventStoreSettings)

    override suspend fun appendToStream(
        streamName: StreamName, eventData: EventData, options: AppendToStreamOptions
    ): WriteResult = client.appendToStream(streamName.value, options, eventData).await()

    override suspend fun readByCorrelationId(id: UUID, maxCount: Int, direction: Direction): ReadResult =
        readStream(StreamName("\$bc-$id"), maxCount, direction)

    override suspend fun readStream(
        streamName: StreamName,
        maxCount: Int,
        direction: Direction,
        resolveLinksTo: Boolean
    ): ReadResult =
        client.readStream(
            streamName.value,
            ReadStreamOptions.get().resolveLinkTos(resolveLinksTo).maxCount(maxCount.toLong()).direction(direction)
        ).await()

    override suspend fun readAll(maxCount: Int, direction: Direction, resolveLinksTo: Boolean): ReadResult =
        client.readAll(ReadAllOptions.get().resolveLinkTos(resolveLinksTo).maxCount(maxCount.toLong()).direction(direction))
            .await()

    override suspend fun readAllByEventType(eventType: EventType, maxCount: Int, direction: Direction): ReadResult =
        readStream(StreamName("\$et-${eventType.value}"), maxCount, direction, resolveLinksTo = true)

    override suspend fun readAllByEventCategory(
        eventCategory: EventCategory,
        maxCount: Int,
        direction: Direction
    ): ReadResult =
        readStream(StreamName("\$ce-${eventCategory.value}"), maxCount, direction, resolveLinksTo = true)

    override suspend fun subscribeByCorrelationId(id: UUID, listener: EventListener): Subscription =
        subscribeToStream(StreamName("\$bc-$id"), listener)

    override suspend fun subscribeToStream(streamName: StreamName, listener: EventListener): Subscription =
        subscribeToStream(streamName, SubscribeToStreamOptions.get(), listener)

    override suspend fun subscribeToStream(
        streamName: StreamName, options: SubscribeToStreamOptions, listener: EventListener
    ): Subscription = subscriptionContext.let { context ->
        client.subscribeToStream(
            streamName.value, object : SubscriptionListener() {
                override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                    streamRevisionBySubscriptionId[subscription.subscriptionId] = event.originalEvent.revision
                    launch(context + SupervisorJob()) { listener(event) }
                }

                override fun onError(subscription: Subscription?, throwable: Throwable) {
                    launch(context + SupervisorJob()) {
                        if (config.reSubscribeOnDrop && subscription != null) {
                            val revision = streamRevisionBySubscriptionId[subscription.subscriptionId] ?: 0
                            subscribeToStream(
                                streamName, options.fromRevision(revision), listener
                            )
                        }
                        config.subscriptionErrorListener(subscription, throwable)
                    }
                }
            }, options
        ).await()
    }

    override suspend fun subscribeToPersistentStream(
        streamName: StreamName, customerGroup: CustomerGroup, listener: PersistentEventListener
    ): PersistentSubscription =
        subscribeToPersistentStream(streamName, customerGroup, PersistentSubscriptionOptions(), listener)

    override suspend fun subscribeToPersistentStream(
        streamName: StreamName,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription = subscriptionContext.let { context ->
        persistentClient.subscribeToStream(
            streamName.value,
            customerGroup.value,
            options.subscriptionOptions,
            object : PersistentSubscriptionListener() {
                override fun onEvent(subscription: PersistentSubscription, retryCount: Int, event: ResolvedEvent) {
                    launch(context + SupervisorJob()) {
                        runCatching {
                            listener(subscription, event)
                            if (options.autoAcknowledge) {
                                subscription.ack(event)
                            }
                        }.onFailure { error ->
                            val eventId = event.originalEvent.eventId
                            if (retryCount < options.maxRetries) {
                                config.logger.error("Error when processing event with id: ${eventId}. Retry attempt [${retryCount + 1}/${options.maxRetries}]. StackTrace: ${error.stackTraceToString()}")
                                subscription.nack(
                                    NackAction.Retry, "exception_${error::class.simpleName}", event
                                )
                            } else {
                                config.logger.error("Error when processing event with id: ${eventId}. Going to ${options.nackAction.name} event. StackTrace: ${error.stackTraceToString()}")
                                subscription.nack(
                                    options.nackAction, "exception_${error::class.simpleName}", event
                                )
                            }
                        }
                    }
                }

                override fun onError(subscription: PersistentSubscription?, throwable: Throwable) {
                    launch(context + SupervisorJob()) {
                        val groupNotFound =
                            (throwable as? StatusRuntimeException)?.status?.code == Status.NOT_FOUND.code
                        if (groupNotFound && options.autoCreateStreamGroup) {
                            config.logger.warn("Stream group $customerGroup not found. AutoCreateStreamGroup is ON. Trying to create the group.")
                            persistentClient.createToStream(
                                streamName.value,
                                customerGroup.value,
                                options.createPersistentSubscriptionToStreamOptions
                            ).await()

                            subscribeToPersistentStream(
                                streamName, customerGroup, options, listener
                            )
                        } else if (options.reSubscribeOnDrop && subscription != null) {
                            subscribeToPersistentStream(
                                streamName, customerGroup, options, listener
                            )
                        }
                        config.persistentSubscriptionErrorEventListener(subscription, throwable)
                    }
                }
            },
        ).await()
    }

    override suspend fun subscribeToAll(listener: EventListener): Subscription =
        subscribeToAll(SubscribeToAllOptions.get(), listener)

    override suspend fun subscribeToAll(options: SubscribeToAllOptions, listener: EventListener): Subscription =
        subscriptionContext.let { context ->
            client.subscribeToAll(
                object : SubscriptionListener() {
                    override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                        event.position.ifPresent { position ->
                            positionBySubscriptionId[subscription.subscriptionId] = position
                        }
                        launch(context + SupervisorJob()) { listener(event) }
                    }

                    override fun onError(subscription: Subscription?, throwable: Throwable) {
                        launch(context + SupervisorJob()) {
                            if (config.reSubscribeOnDrop && subscription != null) {
                                subscribeToAll(
                                    options.apply {
                                        positionBySubscriptionId[subscription.subscriptionId]?.let { fromPosition(it) }
                                    },
                                    listener
                                )
                            }
                            config.subscriptionErrorListener(subscription, throwable)
                        }
                    }
                }, options
            ).await()
        }

    override suspend fun subscribeByStreamNameFiltered(prefix: Prefix, listener: EventListener): Subscription =
        SubscriptionFilter.newBuilder().addStreamNamePrefix(prefix.value).build()
            .let { subscribeToAll(SubscribeToAllOptions.get().filter(it), listener) }

    override suspend fun subscribeByStreamNameFiltered(regex: Regex, listener: EventListener): Subscription =
        SubscriptionFilter.newBuilder().withStreamNameRegularExpression(regex.pattern).build()
            .let { subscribeToAll(SubscribeToAllOptions.get().filter(it), listener) }

    override suspend fun subscribeByEventType(eventType: EventType, listener: EventListener): Subscription =
        SubscriptionFilter.newBuilder().addEventTypePrefix(eventType.value).build()
            .let { subscribeToAll(SubscribeToAllOptions.get().filter(it), listener) }

    override suspend fun subscribeByEventCategory(eventCategory: EventCategory, listener: EventListener): Subscription =
        subscribeToStream(StreamName("\$ce-${eventCategory.value}"), listener)


    override suspend fun subscribePersistentByEventType(
        eventType: EventType,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription =
        subscribeToPersistentStream(
            StreamName("\$et-${eventType.value}"),
            customerGroup,
            PersistentSubscriptionOptions(),
            listener
        )

    override suspend fun subscribePersistentByEventType(
        eventType: EventType,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription =
        subscribeToPersistentStream(
            StreamName("\$et-${eventType.value}"),
            customerGroup,
            options,
            listener
        )

    override suspend fun subscribePersistentByEventCategory(
        eventCategory: EventCategory,
        customerGroup: CustomerGroup,
        listener: PersistentEventListener
    ): PersistentSubscription =
        subscribeToPersistentStream(
            StreamName("\$ce-${eventCategory.value}"),
            customerGroup,
            PersistentSubscriptionOptions(),
            listener
        )

    override suspend fun subscribePersistentByEventCategory(
        eventCategory: EventCategory,
        customerGroup: CustomerGroup,
        options: PersistentSubscriptionOptions,
        listener: PersistentEventListener
    ): PersistentSubscription =
        subscribeToPersistentStream(
            StreamName("\$ce-${eventCategory.value}"),
            customerGroup,
            options,
            listener
        )

    override suspend fun deleteStream(streamName: StreamName): DeleteResult =
        client.deleteStream(streamName.value).await()

    override suspend fun deleteStream(streamName: StreamName, options: DeleteStreamOptions.() -> Unit): DeleteResult =
        client.deleteStream(streamName.value, DeleteStreamOptions.get().apply(options)).await()

    private val subscriptionContextCounter = AtomicInteger(0)

    fun shutdown() =
        parent.complete().also { client.shutdown() }

    @OptIn(DelicateCoroutinesApi::class)
    private val subscriptionContext: ExecutorCoroutineDispatcher
        get() = newSingleThreadContext("EventStoreDB-subscription-context-${subscriptionContextCounter.incrementAndGet()}")
}