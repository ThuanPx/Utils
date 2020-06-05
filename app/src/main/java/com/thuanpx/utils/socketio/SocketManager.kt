package com.thuanpx.utils.socketio

import com.google.gson.Gson
import com.thuanpx.utils.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber


interface SocketManager {

    fun config()

    fun connect()

    fun isConnected(): Boolean

    fun disconnect()

    fun on(eventName: String, listener: Emitter.Listener)

    fun emit(eventName: String, data: String)

    fun off(eventName: String, listener: Emitter.Listener)

    fun setReceivedMessageInternal(listener: ((message: String) -> Unit)?)

    fun setReceivedMessageExternal(listener: ((message: String) -> Unit)?)

    fun unSubscribeMessageListener()

    fun emitExternalMessage()
}

class SocketManagerImpl : SocketManager {

    private var socket: Socket? = null
    private var onReceivedMessageExternal: ((message: String) -> Unit)? = null
    private var onReceivedMessageInternal: ((message: String) -> Unit)? = null

    override fun config() {
        val token = ""
        this.socket = IO.socket(BuildConfig.SOCKET_IO_URL, getSocketConfigs(token)) ?: return
        socket?.let { socket ->
            socket.on(Socket.EVENT_CONNECT) { this.onConnected(it) }
                .on(Socket.EVENT_CONNECTING) { this.onConnecting(it) }
                .on(Socket.EVENT_RECONNECT) { this.onReconnect(it) }
                .on(Socket.EVENT_DISCONNECT) { this.onDisconnected(it) }
                .on(Socket.EVENT_CONNECT_TIMEOUT) { this.onConnectedTimeout(it) }
                .on(Socket.EVENT_ERROR) { this.onError(it) }
                .on(EVENT_MESSAGE_EXTERNAL) { this.onReceivedMessageExternal(it) }
                .on(EVENT_SUBSCRIBE) { this.onSubscribeChannel(it) }
                .on(EVENT_MESSAGE_INTERNAL) { this.onReceivedMessageInternal(it) }
        }
    }

    override fun emitExternalMessage() {
        val token = ""
        socket?.emit(EVENT_SUBSCRIBE, this.getDataExternal(token))
        socket?.emit(EVENT_SUBSCRIBE, this.getDataInternal(token))
    }

    private fun onSubscribeChannel(vararg args: Array<Any>) {
        Timber.e("onSubscribeChannel: ${args[KEY_HEADER]}")
    }

    private fun getSocketConfigs(token: String): IO.Options {
        val opts = IO.Options()
        opts.forceNew = true
        opts.secure = true
        opts.reconnection = true
        opts.upgrade = false
        opts.transports = arrayOf(WebSocket.NAME)
        opts.timeout = 10000
        opts.query = "$AUTHORIZATION=Bearer $token"
        return opts
    }

    override fun connect() {
        socket?.let { socket ->
            if (socket.connected().not()) {
                this.socket?.connect()
            }
        }
    }

    override fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    override fun disconnect() {
        this.socket?.disconnect()
    }

    override fun on(eventName: String, listener: Emitter.Listener) {
        this.socket?.on(eventName, listener)
    }

    override fun emit(eventName: String, data: String) {
        this.socket?.emit(eventName, data)
    }

    override fun off(eventName: String, listener: Emitter.Listener) {
        this.socket?.off(eventName, listener)
    }

    override fun setReceivedMessageInternal(listener: ((message: String) -> Unit)?) {
        onReceivedMessageInternal = listener
    }

    override fun setReceivedMessageExternal(listener: ((message: String) -> Unit)?) {
        onReceivedMessageExternal = listener
    }

    override fun unSubscribeMessageListener() {
        onReceivedMessageInternal = null
        onReceivedMessageExternal = null
    }

    private fun onConnected(vararg args: Array<Any>) {
        Timber.e("onConnected: ${args.contentToString()}")
        emitExternalMessage()
    }

    private fun onReconnect(vararg args: Array<Any>) {
        Timber.e("onReconnect: ${args.contentToString()}")
    }

    private fun onConnecting(vararg args: Array<Any>) {
        Timber.e("onConnecting: ${args[KEY_HEADER]}")
    }

    private fun onDisconnected(vararg args: Array<Any>) {
        Timber.e("onDisconnected: ${args[KEY_HEADER]}")
    }

    private fun onConnectedTimeout(vararg args: Array<Any>) {
        Timber.e("onConnectedTimeout: ${args[KEY_HEADER]}")
    }

    private fun onError(vararg args: Array<Any>) {
        Timber.e("onError: %s", args[0][0])
    }

    private fun onReceivedMessageExternal(vararg args: Array<Any>) {
        Timber.e("onReceivedMessage.... ${args[KEY_HEADER][KEY_DATA]}")
        if (args.isEmpty()) {
            return
        }

        val jsonStr = args[KEY_HEADER][KEY_DATA].toString()

        val jsonObject = JSONObject(jsonStr)
        if (!jsonObject.has(EXTERNAL_MESSAGE)) {
            Timber.e("Wrong type$jsonObject")
            return
        }

        try {
            val jsonData = jsonObject.getJSONObject(EXTERNAL_MESSAGE).toString()

            val message = Gson().fromJson(jsonData, String::class.java)
            onReceivedMessageExternal?.invoke(message)
        } catch (e: JSONException) {
            Timber.e(toString())
        }
    }

    private fun onReceivedMessageInternal(vararg args: Array<Any>) {
        Timber.e("onReceivedMessage.... ${args[KEY_HEADER][KEY_DATA]}")
        if (args.isEmpty()) {
            return
        }

        val jsonStr = args[KEY_HEADER][KEY_DATA].toString()

        val jsonObject = JSONObject(jsonStr)
        if (!jsonObject.has(INTERNAL_MESSAGE)) {
            Timber.e("Wrong type$jsonObject")
            return
        }

        try {
            val jsonData = jsonObject.getJSONObject(INTERNAL_MESSAGE).toString()

            val message = Gson().fromJson(jsonData, String::class.java)
            onReceivedMessageInternal?.invoke(message)
        } catch (e: JSONException) {
            Timber.e(toString())
        }
    }

    private fun getDataExternal(token: String): JSONObject? {
        val requestData = JSONObject()
        val auth = JSONObject()
        val header = JSONObject()

        try {
            val channel = "channelID"
            requestData.put(CHANNEL, channel)
            header.put(AUTHORIZATION, "Bearer $token")

            auth.put(HEADER, header)
            requestData.put(AUTH, auth)

            Timber.e("requestData external: $requestData")
            return requestData
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return null
    }

    private fun getDataInternal(token: String): JSONObject? {
        val requestData = JSONObject()
        val auth = JSONObject()
        val header = JSONObject()

        try {
            val channel = "channelID"
            requestData.put(CHANNEL, channel)
            header.put(AUTHORIZATION, "Bearer $token")

            auth.put(HEADER, header)
            requestData.put(AUTH, auth)

            Timber.e("requestData internal: $requestData")
            return requestData
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return null
    }

    companion object {
        private const val KEY_HEADER = 0
        private const val KEY_DATA = 1
        private const val CHANNEL = "channel"
        private const val AUTHORIZATION = "Authorization"
        private const val HEADER = "headers"
        private const val AUTH = "auth"
        private const val EXTERNAL_MESSAGE = "externalMessage"
        private const val INTERNAL_MESSAGE = "internalMessage"
        private const val EVENT_MESSAGE_EXTERNAL = "manager.external-message"
        private const val EVENT_MESSAGE_INTERNAL = "manager.internal-message"
        private const val EVENT_SUBSCRIBE = "subscribe"
    }
}
