import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';

import '../models/signaling_message.dart';
import '../services/socket_signaling_client.dart';
import '../services/webrtc_session_manager.dart';

class CallScreen extends StatefulWidget {
  const CallScreen({
    super.key,
    required this.role,
    required this.roomId,
    required this.serverUrl,
  });

  final String role;
  final String roomId;
  final String serverUrl;

  @override
  State<CallScreen> createState() => _CallScreenState();
}

class _CallScreenState extends State<CallScreen> {
  final _localRenderer = RTCVideoRenderer();
  final _remoteRenderer = RTCVideoRenderer();
  final _sessionManager = WebRtcSessionManager();
  final _signalingClient = SocketSignalingClient();

  StreamSubscription<SignalingMessage>? _messageSub;
  bool _isMuted = false;
  bool _started = false;
  bool _offerSent = false;
  bool _isOfferer = false;
  bool _isRecoveringIce = false;
  String _status = 'Initializing call...';

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    await _localRenderer.initialize();
    await _remoteRenderer.initialize();

    setState(() => _status = 'Opening camera and microphone...');
    final local = await _sessionManager.openUserMedia();
    _localRenderer.srcObject = local;

    setState(() => _status = 'Creating peer connection...');
    await _sessionManager.createPeerConnection(
      onIceCandidate: (candidate) {
        _signalingClient.sendCandidate(
          widget.roomId,
          candidate.candidate ?? '',
          candidate.sdpMid,
          candidate.sdpMLineIndex,
        );
      },
      onConnectionStateChange: (state) async {
        if (state == RTCPeerConnectionState.RTCPeerConnectionStateFailed) {
          if (_started && _isOfferer && !_isRecoveringIce) {
            _isRecoveringIce = true;
            setState(() => _status = 'Connection failed, restarting ICE...');
            try {
              final restartOffer = await _sessionManager.createIceRestartOffer();
              _signalingClient.sendOffer(widget.roomId, restartOffer.sdp ?? '');
              setState(() => _status = 'ICE restart offer sent');
            } catch (e) {
              setState(() => _status = 'ICE restart error: $e');
            }
            _isRecoveringIce = false;
          } else {
            setState(() => _status = 'Connection failed');
          }
          return;
        }
        setState(() => _status = _mapConnectionState(state));
      },
      onRemoteStream: (remote) {
        setState(() => _remoteRenderer.srcObject = remote);
      },
    );

    _messageSub = _signalingClient.messages.listen((message) async {
      await _handleSignalingMessage(message);
    });

    setState(() => _status = 'Connecting signaling server...');
    await _signalingClient.connect(
      serverUrl: widget.serverUrl,
      roomId: widget.roomId,
      onConnected: () => setState(() => _status = 'Joined room. Waiting for partner...'),
      onReady: () async {
        if (_offerSent) return;
        _offerSent = true;
        _isOfferer = true;
        setState(() => _status = 'Creating offer...');
        try {
          final offer = await _sessionManager.createOffer();
          _signalingClient.sendOffer(widget.roomId, offer.sdp ?? '');
          setState(() {
            _status = 'Offer sent, waiting for answer';
            _started = true;
          });
        } catch (e) {
          _offerSent = false;
          setState(() => _status = 'Offer error: $e');
        }
      },
      onError: (error) => setState(() => _status = error),
    );
  }

  String _mapConnectionState(RTCPeerConnectionState state) {
    switch (state) {
      case RTCPeerConnectionState.RTCPeerConnectionStateConnecting:
        return 'Connecting';
      case RTCPeerConnectionState.RTCPeerConnectionStateConnected:
        return 'Connected';
      case RTCPeerConnectionState.RTCPeerConnectionStateDisconnected:
        return 'Disconnected';
      case RTCPeerConnectionState.RTCPeerConnectionStateClosed:
        return 'Closed';
      case RTCPeerConnectionState.RTCPeerConnectionStateNew:
        return 'New';
      case RTCPeerConnectionState.RTCPeerConnectionStateFailed:
        return 'Failed';
    }
  }

  Future<void> _handleSignalingMessage(SignalingMessage message) async {
    try {
      switch (message.type) {
        case 'offer':
          final remoteSdp = message.sdp;
          if (remoteSdp == null) return;
          setState(() => _status = 'Setting remote offer...');
          await _sessionManager.setRemoteDescription('offer', remoteSdp);
          setState(() => _status = 'Creating answer...');
          final answer = await _sessionManager.createAnswer();
          _signalingClient.sendAnswer(widget.roomId, answer.sdp ?? '');
          setState(() {
            _status = 'Answer sent';
            _started = true;
          });
          break;
        case 'answer':
          final remoteSdp = message.sdp;
          if (remoteSdp == null) return;
          setState(() => _status = 'Setting remote answer...');
          await _sessionManager.setRemoteDescription('answer', remoteSdp);
          setState(() => _status = 'Connecting...');
          break;
        case 'candidate':
          if (message.candidate == null || message.sdpMLineIndex == null) return;
          await _sessionManager.addCandidate(
            RTCIceCandidate(
              message.candidate!,
              message.sdpMid,
              message.sdpMLineIndex!,
            ),
          );
          break;
      }
    } catch (e) {
      setState(() => _status = 'Signaling error: $e');
    }
  }

  @override
  void dispose() {
    _messageSub?.cancel();
    _signalingClient.dispose();
    _sessionManager.dispose();
    _localRenderer.dispose();
    _remoteRenderer.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Video Consultation | ${widget.role} | ${widget.roomId}'),
      ),
      body: Stack(
        children: [
          Positioned.fill(
            child: Container(
              color: Colors.black,
              child: RTCVideoView(
                _remoteRenderer,
                objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitCover,
              ),
            ),
          ),
          Positioned(
            right: 16,
            bottom: 120,
            width: 120,
            height: 180,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: RTCVideoView(
                _localRenderer,
                mirror: true,
                objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitCover,
              ),
            ),
          ),
          Positioned(
            top: 12,
            left: 12,
            right: 12,
            child: Container(
              padding: const EdgeInsets.all(12),
              color: Colors.black54,
              child: Text(
                'Status: $_status',
                style: const TextStyle(color: Colors.white),
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 24),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  FloatingActionButton(
                    heroTag: 'mute',
                    onPressed: () {
                      final micEnabled = _sessionManager.toggleMute();
                      setState(() => _isMuted = !micEnabled);
                    },
                    child: Icon(_isMuted ? Icons.mic_off : Icons.mic),
                  ),
                  const SizedBox(width: 16),
                  FloatingActionButton(
                    heroTag: 'hangup',
                    backgroundColor: Colors.red,
                    onPressed: () async {
                      await _sessionManager.hangUp();
                      if (mounted) Navigator.of(context).pop();
                    },
                    child: const Icon(Icons.call_end),
                  ),
                  const SizedBox(width: 16),
                  FloatingActionButton(
                    heroTag: 'switch',
                    onPressed: () => _sessionManager.switchCamera(),
                    child: const Icon(Icons.cameraswitch),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
