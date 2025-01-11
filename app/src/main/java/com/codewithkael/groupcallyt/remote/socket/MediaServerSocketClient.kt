package com.codewithkael.groupcallyt.remote.socket


import com.codewithkael.groupcallyt.webrtc.MediaServerMessageModel
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class MediaServerSocketClient(
    private val gson: Gson
) {
    private var socketServer: WebSocketClient? = null
    fun init(
        socketUrl: String,
        listener: MediaSocketCallback,
    ) {
        if (socketServer == null) {
            socketServer = object :
                WebSocketClient(URI(socketUrl)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    listener.onRemoteSocketClientOpened()
                }

                override fun onMessage(message: String?) {
                    runCatching {
                        gson.fromJson(message.toString(), MediaServerMessageModel::class.java)
                    }.onSuccess {
                        listener.onRemoteSocketClientNewMessage(it)
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    listener.onRemoteSocketClientClosed()
                }

                override fun onError(ex: Exception?) {
                    listener.onRemoteSocketClientConnectionError(ex)
                }
            }.apply {
                connect()
            }
        }
    }

    fun sendDataToHost(data: Any) {
        runCatching {
            socketServer?.send(gson.toJson(data))
        }
    }

    fun close(){
        socketServer?.close()
    }

    interface MediaSocketCallback {
        fun onRemoteSocketClientOpened()
        fun onRemoteSocketClientClosed()
        fun onRemoteSocketClientConnectionError(e: Exception?)
        fun onRemoteSocketClientNewMessage(message: MediaServerMessageModel)
    }

}