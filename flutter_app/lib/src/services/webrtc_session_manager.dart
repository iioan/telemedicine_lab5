import 'dart:async';

import 'package:flutter_webrtc/flutter_webrtc.dart';
import '../utils/iterable_extensions.dart';

class WebRtcSessionManager {
  RTCPeerConnection? _peerConnection;
  MediaStream? _localStream;
  MediaStream? _remoteStream;
  final List<RTCIceCandidate> _pendingCandidates = [];
  bool _isRemoteDescriptionSet = false;

  Future<MediaStream> openUserMedia() async {
    _localStream = await navigator.mediaDevices.getUserMedia({
      'audio': true,
      'video': {'facingMode': 'user'},
    });
    return _localStream!;
  }

  Future<void> initializePeerConnection({
    required void Function(RTCIceCandidate candidate) onIceCandidate,
    required void Function(RTCPeerConnectionState state) onConnectionStateChange,
    required void Function(MediaStream remoteStream) onRemoteStream,
  }) async {
    final config = {
      'iceServers': [
        {'urls': 'stun:stun.l.google.com:19302'},
        {'urls': 'stun:stun1.l.google.com:19302'},
      ],
      'sdpSemantics': 'unified-plan',
    };

    _peerConnection = await createPeerConnection(config);
    _remoteStream = await createLocalMediaStream('REMOTE_STREAM');

    for (final track in _localStream?.getTracks() ?? <MediaStreamTrack>[]) {
      await _peerConnection?.addTrack(track, _localStream!);
    }

    _peerConnection?.onIceCandidate = (candidate) {
      onIceCandidate(candidate);
    };

    _peerConnection?.onConnectionState = (state) {
      onConnectionStateChange(state);
    };

    _peerConnection?.onTrack = (event) {
      if (event.streams.isNotEmpty) {
        onRemoteStream(event.streams.first);
      } else if (event.track.kind == 'video') {
        _remoteStream?.addTrack(event.track);
        onRemoteStream(_remoteStream!);
      }
    };
  }

  Future<RTCSessionDescription> createOffer() async {
    final offer = await _peerConnection!.createOffer();
    await _peerConnection!.setLocalDescription(offer);
    return offer;
  }

  Future<RTCSessionDescription> createIceRestartOffer() async {
    final offer = await _peerConnection!.createOffer({'iceRestart': true});
    await _peerConnection!.setLocalDescription(offer);
    return offer;
  }

  Future<RTCSessionDescription> createAnswer() async {
    final answer = await _peerConnection!.createAnswer();
    await _peerConnection!.setLocalDescription(answer);
    return answer;
  }

  Future<void> setRemoteDescription(String type, String sdp) async {
    await _peerConnection!.setRemoteDescription(RTCSessionDescription(sdp, type));
    _isRemoteDescriptionSet = true;
    for (final candidate in _pendingCandidates) {
      await _peerConnection!.addCandidate(candidate);
    }
    _pendingCandidates.clear();
  }

  Future<void> addCandidate(RTCIceCandidate candidate) async {
    if (_isRemoteDescriptionSet) {
      await _peerConnection?.addCandidate(candidate);
    } else {
      _pendingCandidates.add(candidate);
    }
  }

  bool toggleMute() {
    final audioTrack = _localStream?.getAudioTracks().firstOrNull;
    if (audioTrack == null) return true;
    audioTrack.enabled = !audioTrack.enabled;
    return audioTrack.enabled;
  }

  Future<void> switchCamera() async {
    final videoTrack = _localStream?.getVideoTracks().firstOrNull;
    if (videoTrack != null) {
      await Helper.switchCamera(videoTrack);
    }
  }

  Future<void> hangUp() async {
    await _peerConnection?.close();
    _peerConnection = null;
    _isRemoteDescriptionSet = false;
    _pendingCandidates.clear();

    await _localStream?.dispose();
    _localStream = null;
    await _remoteStream?.dispose();
    _remoteStream = null;
  }

  Future<void> dispose() => hangUp();
}
