package com.codewithkael.groupcallyt.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.codewithkael.groupcallyt.remote.socket.SignallingSocketClient
import com.codewithkael.groupcallyt.utils.MyApplication
import com.codewithkael.groupcallyt.utils.SignalServerMessageModel
import com.codewithkael.groupcallyt.utils.UsersList
import com.codewithkael.groupcallyt.webrtc.MediaServerMessageModel
import com.codewithkael.groupcallyt.webrtc.StreamClient
import com.codewithkael.groupcallyt.webrtc.StreamPublisher
import com.codewithkael.groupcallyt.webrtc.WebRTCFactory
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import java.util.stream.Stream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val webRTCFactory: WebRTCFactory,
    private val signallingSocketClient: SignallingSocketClient,
    private val gson: Gson
) : ViewModel() {

    private var localUserId: String = ""
    private var localUserName: String = ""

    //state to hold media streams keyed by usernames to update compose
    val mediaStreamsMap = mutableStateOf<Map<String, MediaStream>>(emptyMap())
    private val streamClientsMap = mutableMapOf<String, StreamClient>()
    private var streamPublisher: StreamPublisher? = null


    fun joinRoom(localSurface: SurfaceViewRenderer, roomName: String, userName: String) {
        webRTCFactory.prepareLocalStream(localSurface, object : WebRTCFactory.LocalStreamListener {
            override fun onLocalStreamReady(mediaStream: MediaStream) {
                mediaStream.videoTracks[0]?.let {
                    it.addSink(localSurface)
                    //time to connect to backend
                    openSignallingSocket(userName, roomName)
                }
            }
        })
    }

    fun prepareRemoteSurface(remoteSurface: SurfaceViewRenderer) {
        webRTCFactory.initSurfaceView(remoteSurface)
    }

    private fun openSignallingSocket(userName: String, roomName: String) {
        localUserId = userName + MyApplication.STREAM_ID
        localUserName = userName
        signallingSocketClient.init(roomName,
            userName,
            localUserId,
            object : SignallingSocketClient.SignalSocketCallback {
                override fun onRemoteSocketClientOpened() {
                    //open the connection to media server and publish the local stream
                    openMediaServerSocket()
                }

                override fun onRemoteSocketClientClosed() {
                    Log.d("TAG", "onRemoteSocketClientClosed: ")
                }

                override fun onRemoteSocketClientConnectionError(e: Exception?) {
                    Log.d("TAG", "onRemoteSocketClientConnectionError: ${e?.message}")
                }

                override fun onRemoteSocketClientNewMessage(message: SignalServerMessageModel) {
                    Log.d("TAG", "onRemoteSocketClientNewMessage: $message")
                    handleSignallingMessage(message)
                }

            })
    }


    private fun openMediaServerSocket() {
        streamPublisher =
            StreamPublisher(webRTCFactory, gson, object : StreamPublisher.StreamPublisherCallBack {
                override fun onMessageGenerated(signalServerMessageModel: SignalServerMessageModel) {
                    Log.d("TAG", "onMessageGenerated: $signalServerMessageModel")
                    signallingSocketClient.sendMessage(signalServerMessageModel)
                }
            })

        streamPublisher?.startPublishing(localUserId, localUserName)
    }

    private fun handleSignallingMessage(message: SignalServerMessageModel) {
        when (message.command) {
            "room_users" -> handleRoomUsers(message)
            "user_connected" -> handleUserConnected(message)
        }
    }

    private fun handleRoomUsers(message: SignalServerMessageModel) {
        runCatching {
            val userList = gson.fromJson(message.data.toString(), UsersList::class.java)
            userList.users.forEach { user ->
                StreamClient(webRTCFactory, gson, object : StreamClient.StreamClientCallBack {
                    override fun onMessageGenerated(mediaServerMessageModel: MediaServerMessageModel) {

                    }

                    override fun onStreamReady(mediaStream: MediaStream, username: String) {
                        mediaStreamsMap.value += (username to mediaStream)
                    }

                    override fun onClientDisconnected(username: String) {
                        mediaStreamsMap.value -= username
                        streamClientsMap.remove(username)
                    }

                }).also {
                    it.startClient(user.userId, user.userName)
                    streamClientsMap[user.userName] = it
                }
            }
        }
    }

    private fun handleUserConnected(message: SignalServerMessageModel) {
        val client = StreamClient(webRTCFactory,gson, object : StreamClient.StreamClientCallBack {
            override fun onMessageGenerated(mediaServerMessageModel: MediaServerMessageModel) {

            }

            override fun onStreamReady(mediaStream: MediaStream, username: String) {
                mediaStreamsMap.value += (username to mediaStream)
            }

            override fun onClientDisconnected(username: String) {
                streamClientsMap.remove(username)
                mediaStreamsMap.value -= username
            }
        }).also {
            it.startClient(message.data.toString(),message.sender.toString())
        }
        streamClientsMap[message.sender.toString()] = client
    }


    override fun onCleared() {
        super.onCleared()
        streamPublisher?.destroy()
        streamClientsMap.forEach { (_, client) ->
            client.destroy()
        }
        signallingSocketClient.disconnect()
        webRTCFactory.onDestroy()
    }
}