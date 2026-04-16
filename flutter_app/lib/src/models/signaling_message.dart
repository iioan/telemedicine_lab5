class SignalingMessage {
  const SignalingMessage({
    required this.type,
    this.sdp,
    this.candidate,
    this.sdpMid,
    this.sdpMLineIndex,
  });

  final String type;
  final String? sdp;
  final String? candidate;
  final String? sdpMid;
  final int? sdpMLineIndex;

  factory SignalingMessage.fromMap(Map<String, dynamic> map) {
    return SignalingMessage(
      type: map['type']?.toString() ?? '',
      sdp: map['sdp']?.toString(),
      candidate: map['candidate']?.toString(),
      sdpMid: map['sdpMid']?.toString(),
      sdpMLineIndex: _parseInt(map['sdpMLineIndex']),
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    return int.tryParse(value.toString());
  }
}
