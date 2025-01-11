package com.codewithkael.groupcallyt.webrtc

import com.codewithkael.groupcallyt.remote.socket.MediaServerSocketClient
import com.codewithkael.groupcallyt.utils.Constants.getStreamPath
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

class StreamClient constructor(
    private val webRTCFactory: WebRTCFactory,
    private val gson: Gson,
    private val callBack: StreamClientCallBack
) {

    private var mediaServerSocket: MediaServerSocketClient? = null
    private var rtcClient:RTCClient?=null

    fun startClient(clientUserId: String, clientUsername: String) {
        mediaServerSocket = MediaServerSocketClient(gson)
        mediaServerSocket?.init(getStreamPath(clientUserId),
            object :MediaServerSocketClient.MediaSocketCallback{
                override fun onRemoteSocketClientOpened() {
                    mediaServerSocket?.sendDataToHost(
                        MediaServerMessageModel(
                            command = "request_offer"
                        )
                    )
                }

                override fun onRemoteSocketClientClosed() {
                }

                override fun onRemoteSocketClientConnectionError(e: Exception?) {
                }

                override fun onRemoteSocketClientNewMessage(message: MediaServerMessageModel) {
                    if (message.command =="offer"){
                        initRemoteRTCClient(clientUsername)
                        message.sdp?.let {
                            rtcClient?.onRemoteSessionReceived(it.toWebrtcSessionDescription())
                        }
                        message.candidates.forEach { ice->
                            rtcClient?.onIceCandidateReceived(ice.toWebrtcCandidate())
                        }
                        rtcClient?.answer(message.id!!)
                    } else if (message.code == 404){
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(1000)
                            mediaServerSocket?.sendDataToHost(
                                MediaServerMessageModel(command = "request_offer")
                            )
                        }
                    }
                }
            })
    }

    private fun initRemoteRTCClient(clientUsername: String){
        runCatching {
            rtcClient?.onDestroy()
        }
        rtcClient = null
        rtcClient = webRTCFactory.createRTCClient(
            observer = object : MyPeerObserver(){
                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)
                    if (newState == PeerConnection.PeerConnectionState.DISCONNECTED){
                        callBack.onClientDisconnected(clientUsername)
                    }
                }

                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    p0?.let {
                        rtcClient?.onLocalIceCandidateGenerated(it)
                    }
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    callBack.onStreamReady(p0!!,clientUsername)
                }
            },
            listener = object :RTCClientImpl.TransferStreamerDataToServerListener{
                override fun onTransferEventToSocket(data: MediaServerMessageModel) {
                    mediaServerSocket?.sendDataToHost(data)
                }
            },isPublisherMode = false
        )
    }

    fun destroy() {
        mediaServerSocket?.close()
        rtcClient?.onDestroy()
    }

    interface StreamClientCallBack {
        fun onMessageGenerated(mediaServerMessageModel: MediaServerMessageModel)
        fun onStreamReady(mediaStream: MediaStream, username: String)
        fun onClientDisconnected(username: String)
    }
}