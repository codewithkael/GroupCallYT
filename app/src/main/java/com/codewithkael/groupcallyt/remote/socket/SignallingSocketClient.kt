package com.codewithkael.groupcallyt.remote.socket

import com.codewithkael.groupcallyt.utils.Constants
import com.codewithkael.groupcallyt.utils.SignalServerMessageModel
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI
import javax.inject.Inject

class SignallingSocketClient @Inject constructor(
    private val gson: Gson
) {

    private var socket: Socket? = null
    fun init(
        roomName: String, userName: String, userId: String, listener: SignalSocketCallback
    ) {
        try {
            val options = IO.Options()
            options.query = "roomName=$roomName&name=$userName&userId=$userId"
            val uri = URI.create(Constants.getSignallingServerUrl())
            socket = IO.socket(uri, options)
            socket?.on(Socket.EVENT_CONNECT) {
                listener.onRemoteSocketClientOpened()
            }
            socket?.on(Socket.EVENT_DISCONNECT) {
                listener.onRemoteSocketClientClosed()
            }
            socket?.on("message") { args ->
                runCatching {
                    val signalServerMessageModel =
                        gson.fromJson(args[0].toString(), SignalServerMessageModel::class.java)
                    listener.onRemoteSocketClientNewMessage(signalServerMessageModel)
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val e = args.getOrNull(0) as? Exception
                listener.onRemoteSocketClientConnectionError(e)
            }

            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
            listener.onRemoteSocketClientConnectionError(e)
        }

    }

    fun sendMessage(message:Any){
        socket?.emit("message",gson.toJson(message))
    }

    fun disconnect(){
        socket?.disconnect()
    }


    interface SignalSocketCallback {
        fun onRemoteSocketClientOpened()
        fun onRemoteSocketClientClosed()
        fun onRemoteSocketClientConnectionError(e: Exception?)
        fun onRemoteSocketClientNewMessage(message: SignalServerMessageModel)
    }
}