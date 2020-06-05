package com.thuanpx.utils.pusher

import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionStateChange
import com.pusher.client.util.HttpAuthorizer
import com.thuanpx.utils.BuildConfig
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber

interface PusherManager {
    fun connect()
    fun disconnect()
    fun subscribe()
    fun unsubscribe()
    fun bind(eventName: String? = null)
    fun unBind(eventName: String? = null)
    fun pusherEventListener(): PublishSubject<String>
    fun isConnect(): Boolean
    fun configPusher()
}

class PusherImpl(private val token: String) : PusherManager {
    private var pusher: Pusher? = null
    private var channelGlobal: Channel? = null
    private var channelPrivate: Channel? = null
    private var channelNamePrivate: String? = null
    private val compositeDisposable = CompositeDisposable()
    private var channelGlobalEvent: SubscriptionEventListener? = null
    private var channelPrivateEvent: SubscriptionEventListener? = null
    private var isConnected = false
    private val pusherEventListener = PublishSubject.create<String>()

    override fun configPusher() {
        val pusherOptions = PusherOptions().apply {
            token.let {
                authorizer = configHttpAuthorizer()
            }
            setCluster(BuildConfig.PUSHER_CLUSTER)
        }
        pusher = Pusher(BuildConfig.PUSHER_API_KEY, pusherOptions)
    }

    private fun configHttpAuthorizer(): HttpAuthorizer {
        val httpAuthorizer = HttpAuthorizer("url$AUTH_URL")
        val headerHashMap = hashMapOf<String, String>()
        headerHashMap["Authorization"] = "Bearer token"
        httpAuthorizer.setHeaders(headerHashMap)
        return httpAuthorizer
    }

    override fun connect() {
        val connectionEventListener = object : ConnectionEventListener {
            override fun onConnectionStateChange(con: ConnectionStateChange?) {
                Timber.d("Connected ====> $con")
                isConnected = true
            }

            override fun onError(p0: String?, p1: String?, p2: Exception?) {
                Timber.e("Connect Error ====> $p0 $p1 $p2")
                isConnected = false
            }
        }
        pusher?.connect(connectionEventListener)
    }

    override fun disconnect() {
        pusher?.disconnect()
    }

    private fun configChannelGlobal() {
        if (pusher?.getChannel(CHANNEL_GLOBAL) != null) return
        val globalChannelEventListener = object : ChannelEventListener {
            override fun onEvent(event: PusherEvent?) {
                Timber.d("Sub onEvent ====> $event")
            }

            override fun onSubscriptionSucceeded(p0: String?) {
                Timber.d("Sub Success ====> $p0")
            }
        }
        channelGlobal = pusher?.subscribe(CHANNEL_GLOBAL, globalChannelEventListener)
    }

    private fun configChannelPrivate() {
        channelNamePrivate = "channel"

        if (pusher?.getPrivateChannel(channelNamePrivate) != null) return
        val privateChannelEventListener = object : PrivateChannelEventListener {
            override fun onAuthenticationFailure(p0: String?, p1: java.lang.Exception?) {
                Timber.d("Sub Failed ====> $p0 $p1")
            }

            override fun onEvent(event: PusherEvent?) {
                Timber.d("Sub onEvent ====> $event")
            }

            override fun onSubscriptionSucceeded(p0: String?) {
                Timber.d("Sub Success ====> $p0")
            }
        }
        channelPrivate = pusher?.subscribePrivate(channelNamePrivate, privateChannelEventListener)
    }

    override fun subscribe() {
        configChannelGlobal()
        configChannelPrivate()
    }

    override fun unsubscribe() {
        pusher?.unsubscribe(CHANNEL_GLOBAL)
        channelNamePrivate?.let { pusher?.unsubscribe(it) }
        compositeDisposable.clear()
    }

    override fun bind(eventName: String?) {
        val channelGlobalEvent = object : ChannelEventListener {
            override fun onEvent(event: PusherEvent?) {
                Timber.d("Bind eventName ====> $event")
                if (event?.data?.isNotBlank() == true) {
                    pusherEventListener.onNext(event.data)
                }
            }

            override fun onSubscriptionSucceeded(p0: String?) {
                Timber.d("Bind Success ====> $p0")
            }
        }
        channelGlobal?.bind(EVENT_NAME, channelGlobalEvent)

        channelPrivateEvent = object : PrivateChannelEventListener {
            override fun onEvent(event: PusherEvent?) {
                Timber.d("Bind eventName ====> $event")
                if (event?.data?.isNotBlank() == true) {
                    pusherEventListener.onNext(event.data)
                }
            }

            override fun onAuthenticationFailure(p0: String?, p1: Exception?) {
                Timber.d("Bind Failed ====> $p0 $p1")
            }

            override fun onSubscriptionSucceeded(p0: String?) {
                Timber.d("Bind Success ====> $p0")
            }
        }
        channelPrivate?.bind(EVENT_NAME, channelPrivateEvent)
    }

    override fun unBind(eventName: String?) {
        channelGlobalEvent?.let { eventListener ->
            if (channelGlobal?.isSubscribed == true) {
                channelGlobal?.unbind(CHANNEL_GLOBAL, eventListener)
            }
        }

        channelPrivateEvent?.let { eventListener ->
            if (channelPrivate?.isSubscribed == true) {
                channelPrivate?.unbind(channelNamePrivate, eventListener)
            }
        }
    }

    override fun isConnect() = isConnected

    override fun pusherEventListener(): PublishSubject<String> = pusherEventListener

    companion object {
        private const val EVENT_NAME =
            "Illuminate\\Notifications\\Events\\BroadcastNotificationCreated"
        private const val AUTH_URL = "broadcasting/auth"
        private const val CHANNEL_GLOBAL = "smp_order.global"
    }
}
