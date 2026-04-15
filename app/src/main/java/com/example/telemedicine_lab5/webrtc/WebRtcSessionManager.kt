package com.example.telemedicine_lab5.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class WebRtcSessionManager(private val appContext: Context) {
    private val eglBase = EglBase.create()
    private val TAG = "WebRtcSessionManager"

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(appContext)
                .createInitializationOptions(),
        )

        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private var peerConnection: PeerConnection? = null
    private var localVideoCapturer: CameraVideoCapturer? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var pendingRemoteVideoTrack: VideoTrack? = null

    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    private fun buildIceServers(): List<PeerConnection.IceServer> {
        return listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
        )
    }

    fun initRenderers(local: SurfaceViewRenderer, remote: SurfaceViewRenderer) {
        if (localRenderer !== local) {
            localRenderer = local
            local.init(eglBase.eglBaseContext, null)
            local.setEnableHardwareScaler(true)
            local.setMirror(true)
            local.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            // Ensure track is attached if already opened
            localVideoTrack?.addSink(local)
        }

        if (remoteRenderer !== remote) {
            remoteRenderer = remote
            remote.init(eglBase.eglBaseContext, null)
            remote.setEnableHardwareScaler(true)
            remote.setMirror(false)
            remote.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            pendingRemoteVideoTrack?.addSink(remote)
        }
    }

    fun openUserMedia() {
        val videoCapturer = createVideoCapturer()
            ?: throw IllegalStateException("No camera available for WebRTC")

        localVideoCapturer = videoCapturer
        localVideoSource = peerConnectionFactory.createVideoSource(false)
        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", localAudioSource)

        val textureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer.initialize(textureHelper, appContext, localVideoSource?.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack?.setEnabled(true)
        localAudioTrack?.setEnabled(true)
        localRenderer?.let { localVideoTrack?.addSink(it) }
    }

    fun createPeerConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit,
        onRemoteTrack: () -> Unit,
    ) {
        val configuration = PeerConnection.RTCConfiguration(
            buildIceServers(),
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            configuration,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    Log.d(TAG, "onIceCandidate: ${candidate.sdp}")
                    onIceCandidate(candidate)
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    Log.d(TAG, "onConnectionChange: $newState")
                    onConnectionStateChange(newState)
                }

                override fun onSignalingChange(newState: PeerConnection.SignalingState) {
                    Log.d(TAG, "onSignalingChange: $newState")
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "onIceConnectionChange: $newState")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "onIceGatheringChange: $newState")
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
                override fun onAddStream(stream: org.webrtc.MediaStream?) {
                    val videoTrack = stream?.videoTracks?.firstOrNull()
                    if (videoTrack != null) {
                        pendingRemoteVideoTrack = videoTrack
                        remoteRenderer?.let { videoTrack.addSink(it) }
                        onRemoteTrack()
                    }
                }
                override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onDataChannel(dataChannel: org.webrtc.DataChannel?) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out org.webrtc.MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        pendingRemoteVideoTrack = track
                        remoteRenderer?.let { track.addSink(it) }
                        onRemoteTrack()
                    }
                }
            },
        )

        val streamId = "LOCAL_STREAM"
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf(streamId)) }
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf(streamId)) }
    }

    suspend fun createOffer(): SessionDescription {
        Log.d(TAG, "Creating offer")
        val pc = peerConnection ?: error("PeerConnection not created")
        val offer = pc.createSdp(MediaConstraints(), isOffer = true)
        pc.setSdpLocal(offer)
        return offer
    }

    suspend fun createIceRestartOffer(): SessionDescription {
        Log.d(TAG, "Creating ICE restart offer")
        val pc = peerConnection ?: error("PeerConnection not created")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        val offer = pc.createSdp(constraints, isOffer = true)
        pc.setSdpLocal(offer)
        return offer
    }

    suspend fun createAnswer(): SessionDescription {
        Log.d(TAG, "Creating answer")
        val pc = peerConnection ?: error("PeerConnection not created")
        val answer = pc.createSdp(MediaConstraints(), isOffer = false)
        pc.setSdpLocal(answer)
        return answer
    }

    suspend fun setRemoteDescription(type: String, sdp: String) {
        Log.d(TAG, "Setting remote description: $type")
        val sessionType = when (type) {
            "offer" -> SessionDescription.Type.OFFER
            "answer" -> SessionDescription.Type.ANSWER
            else -> throw IllegalArgumentException("Unsupported SDP type: $type")
        }
        peerConnection?.setSdpRemote(SessionDescription(sessionType, sdp))
        
        isRemoteDescriptionSet = true
        drainCandidates()
    }

    fun addCandidate(candidate: IceCandidate) {
        if (isRemoteDescriptionSet) {
            Log.d(TAG, "Adding ICE candidate immediately")
            peerConnection?.addIceCandidate(candidate)
        } else {
            Log.d(TAG, "Queuing ICE candidate")
            synchronized(pendingIceCandidates) {
                pendingIceCandidates.add(candidate)
            }
        }
    }

    private fun drainCandidates() {
        synchronized(pendingIceCandidates) {
            Log.d(TAG, "Draining ${pendingIceCandidates.size} queued candidates")
            pendingIceCandidates.forEach {
                peerConnection?.addIceCandidate(it)
            }
            pendingIceCandidates.clear()
        }
    }

    fun toggleMute(): Boolean {
        val currentEnabled = localAudioTrack?.enabled() ?: true
        localAudioTrack?.setEnabled(!currentEnabled)
        return !currentEnabled
    }

    fun switchCamera() {
        localVideoCapturer?.switchCamera(null)
    }
    
    fun hangUp() {
        peerConnection?.close()
        peerConnection = null
        isRemoteDescriptionSet = false
        synchronized(pendingIceCandidates) { pendingIceCandidates.clear() }

        localVideoCapturer?.stopCaptureSafely()
        localVideoCapturer?.dispose()
        localVideoCapturer = null

        localVideoTrack?.dispose()
        localVideoTrack = null
        localVideoSource?.dispose()
        localVideoSource = null

        localAudioTrack?.dispose()
        localAudioTrack = null
        localAudioSource?.dispose()
        localAudioSource = null

        localRenderer?.clearImage()
        remoteRenderer?.clearImage()
    }

    fun dispose() {
        hangUp()
        localRenderer?.release()
        remoteRenderer?.release()
        eglBase.release()
    }

    private fun createVideoCapturer(): CameraVideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        val frontCamera = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val cameraName = frontCamera ?: enumerator.deviceNames.firstOrNull() ?: return null
        return enumerator.createCapturer(cameraName, null)
    }
}

private suspend fun PeerConnection.createSdp(
    constraints: MediaConstraints,
    isOffer: Boolean,
): SessionDescription = suspendCancellableCoroutine { continuation ->
    val observer = object : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            continuation.resume(sessionDescription)
        }

        override fun onCreateFailure(error: String) {
            continuation.resumeWithException(IllegalStateException(error))
        }

        override fun onSetSuccess() = Unit
        override fun onSetFailure(error: String) = Unit
    }

    if (isOffer) {
        createOffer(observer, constraints)
    } else {
        createAnswer(observer, constraints)
    }
}

private suspend fun PeerConnection.setSdpLocal(sessionDescription: SessionDescription) {
    suspendCancellableCoroutine<Unit> { continuation ->
        setLocalDescription(object : org.webrtc.SdpObserver {
            override fun onSetSuccess() {
                continuation.resume(Unit)
            }

            override fun onSetFailure(error: String) {
                continuation.resumeWithException(IllegalStateException(error))
            }

            override fun onCreateSuccess(sessionDescription: SessionDescription) = Unit
            override fun onCreateFailure(error: String) = Unit
        }, sessionDescription)
    }
}

private suspend fun PeerConnection.setSdpRemote(sessionDescription: SessionDescription) {
    suspendCancellableCoroutine<Unit> { continuation ->
        setRemoteDescription(object : org.webrtc.SdpObserver {
            override fun onSetSuccess() {
                continuation.resume(Unit)
            }

            override fun onSetFailure(error: String) {
                continuation.resumeWithException(IllegalStateException(error))
            }

            override fun onCreateSuccess(sessionDescription: SessionDescription) = Unit
            override fun onCreateFailure(error: String) = Unit
        }, sessionDescription)
    }
}

private fun CameraVideoCapturer.stopCaptureSafely() {
    try {
        stopCapture()
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}
