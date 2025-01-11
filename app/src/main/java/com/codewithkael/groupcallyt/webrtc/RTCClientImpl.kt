package com.codewithkael.groupcallyt.webrtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class RTCClientImpl(
    connection: PeerConnection,
    private val transferListener: TransferStreamerDataToServerListener
) : RTCClient {

    private var remoteMediaServerMessageModel = MediaServerMessageModel(command = "answer")
    private fun resetLocalOffer(id:Int) {
        remoteMediaServerMessageModel = MediaServerMessageModel(command = "answer", id = id)
    }

    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    override val peerConnection: PeerConnection = connection

    override fun answer(id:Int) {
        resetLocalOffer(id)
        peerConnection.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        remoteMediaServerMessageModel.sdp = MySessionDescription(
                            type = "answer",
                            sdp = desc?.description.toString()
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            //wait for gathering the candidates
                            delay(3000)
                            transferListener.onTransferEventToSocket(remoteMediaServerMessageModel)
                        }
                    }
                }, desc)
            }
        }, mediaConstraint)
    }



    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(MySdpObserver(),sessionDescription)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)

    }


    override fun onDestroy() {
        peerConnection.close()

    }

    override fun onLocalIceCandidateGenerated(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)
        remoteMediaServerMessageModel.candidates.add(
            MyIceCandidates(
                candidate = iceCandidate.sdp,
                sdpMLineIndex = iceCandidate.sdpMLineIndex,
                sdpMid = iceCandidate.sdpMid
            )
        )
    }

    interface TransferStreamerDataToServerListener {
        fun onTransferEventToSocket(data: MediaServerMessageModel)
    }
}