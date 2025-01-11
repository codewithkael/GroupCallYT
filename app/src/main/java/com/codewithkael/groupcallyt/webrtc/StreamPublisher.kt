package com.codewithkael.groupcallyt.webrtc

import android.util.Log
import com.codewithkael.groupcallyt.remote.socket.MediaServerSocketClient
import com.codewithkael.groupcallyt.utils.Constants.getPublishStreamPath
import com.codewithkael.groupcallyt.utils.SignalServerMessageModel
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection

class StreamPublisher(
    private val webRTCFactory: WebRTCFactory,
    private val gson: Gson,
    private val callBack: StreamPublisherCallBack
) {

    private var mediaServerSocket: MediaServerSocketClient? = null
    private var rtcClient: RTCClient? = null

    fun startPublishing(senderUsrId: String, senderUserName: String) {
        mediaServerSocket = MediaServerSocketClient(gson)
        mediaServerSocket?.init(getPublishStreamPath(senderUsrId),
            object : MediaServerSocketClient.MediaSocketCallback {
                override fun onRemoteSocketClientOpened() {
                    mediaServerSocket?.sendDataToHost(
                        MediaServerMessageModel(command = "request_offer")
                    )
                }

                override fun onRemoteSocketClientClosed() {
                }

                override fun onRemoteSocketClientConnectionError(e: Exception?) {
                }

                override fun onRemoteSocketClientNewMessage(message: MediaServerMessageModel) {
                    if (message.command == "offer") {
                        initRemoteRTCClient(senderUsrId,senderUserName)
                        message.sdp?.let {
                            rtcClient?.onRemoteSessionReceived(it.toWebrtcSessionDescription())
                        }
                        message.candidates.forEach { ice ->
                            rtcClient?.onIceCandidateReceived(ice.toWebrtcCandidate())
                        }
                    }
                    rtcClient?.answer(message.id!!)
                }

            })
    }

    private fun initRemoteRTCClient(senderUserId: String, senderUserName: String) {
        runCatching { rtcClient?.onDestroy() }
        rtcClient = null
        rtcClient = webRTCFactory.createRTCClient(observer = object : MyPeerObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    rtcClient?.onLocalIceCandidateGenerated(it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                Log.d("TAG", "onConnectionChange: $newState")
                if (newState == PeerConnection.PeerConnectionState.CONNECTED){
                    callBack.onMessageGenerated(
                        SignalServerMessageModel(
                            command = "user_connected",
                            data = senderUserId,
                            sender = senderUserName
                        )
                    )
                }
            }
        }, listener = object : RTCClientImpl.TransferStreamerDataToServerListener {
            override fun onTransferEventToSocket(data: MediaServerMessageModel) {
                mediaServerSocket?.sendDataToHost(data)
            }
        }, isPublisherMode = true)
    }

    fun destroy() {
        mediaServerSocket?.close()
        rtcClient?.onDestroy()
    }

    interface StreamPublisherCallBack {
        fun onMessageGenerated(signalServerMessageModel: SignalServerMessageModel)
    }
}