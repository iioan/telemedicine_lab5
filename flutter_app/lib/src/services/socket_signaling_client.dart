import 'dart:async';

import 'package:socket_io_client/socket_io_client.dart' as io;

import '../models/signaling_message.dart';

class SocketSignalingClient {
  io.Socket? _socket;
  final _messagesController = StreamController<SignalingMessage>.broadcast();

  Stream<SignalingMessage> get messages => _messagesController.stream;

  Future<void> connect({
    required String serverUrl,
    required String roomId,
    required void Function() onConnected,
    required void Function() onReady,
    required void Function(String error) onError,
  }) async {
    disconnect();

    final normalizedUrl = _normalizeServerUrl(serverUrl);
    _socket = io.io(
      normalizedUrl,
      io.OptionBuilder()
          .setPath('/socket.io/')
          .setTransports(['websocket', 'polling'])
          .enableReconnection()
          .setReconnectionAttempts(10)
          .setTimeout(10000)
          .build(),
    );

    _socket!
      ..onConnect((_) {
        _socket?.emit('join', roomId);
        onConnected();
      })
      ..on('ready', (_) => onReady())
      ..on('full', (payload) {
        onError('Room ${payload?.toString() ?? roomId} is full');
      })
      ..onConnectError((e) => onError(e?.toString() ?? 'Socket connection error'))
      ..onError((e) => onError(e?.toString() ?? 'Socket error'))
      ..onDisconnect((reason) => onError('Socket disconnected: $reason'))
      ..on('message', (payload) {
        final normalized = _normalizePayload(payload);
        if (normalized != null) {
          _messagesController.add(SignalingMessage.fromMap(normalized));
        }
      });

    _socket?.connect();
  }

  void sendOffer(String roomId, String sdp) {
    _sendPayload(roomId, {'type': 'offer', 'sdp': sdp});
  }

  void sendAnswer(String roomId, String sdp) {
    _sendPayload(roomId, {'type': 'answer', 'sdp': sdp});
  }

  void sendCandidate(String roomId, String candidate, String? sdpMid, int? sdpMLineIndex) {
    _sendPayload(roomId, {
      'type': 'candidate',
      'candidate': candidate,
      'sdpMid': sdpMid,
      'sdpMLineIndex': sdpMLineIndex,
    });
  }

  void _sendPayload(String roomId, Map<String, dynamic> payload) {
    _socket?.emit('message', {'room': roomId, 'payload': payload});
  }

  Map<String, dynamic>? _normalizePayload(dynamic payload) {
    if (payload is Map) {
      final casted = Map<String, dynamic>.from(payload as Map);
      final nested = casted['payload'];
      if (nested is Map) {
        return Map<String, dynamic>.from(nested as Map);
      }
      return casted;
    }
    return null;
  }

  String _normalizeServerUrl(String serverUrl) {
    final trimmed = serverUrl.trim();
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return trimmed;
    }
    return 'http://$trimmed';
  }

  void disconnect() {
    _socket?.clearListeners();
    _socket?.disconnect();
    _socket = null;
  }

  void dispose() {
    disconnect();
    _messagesController.close();
  }
}
