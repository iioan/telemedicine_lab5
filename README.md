# Telemedicine Lab 5 (Android Compose + WebRTC)

This project rewrites the lab's Flutter WebRTC flow in Kotlin for Android with:

- Single-Activity architecture (`MainActivity` hosts only theme + navigation)
- Compose Navigation (`home` and `call` routes)
- WebRTC media/peer logic split from UI
- Socket.IO signaling client aligned with the provided Node.js server contract
- PiP call UI + doctor command bar (mute/end/switch camera)

## Project Structure

- `app/src/main/java/com/example/telemedicine_lab5/ui/navigation/` - routes and nav graph
- `app/src/main/java/com/example/telemedicine_lab5/ui/screens/` - Compose screens
- `app/src/main/java/com/example/telemedicine_lab5/webrtc/` - `RTCPeerConnection` and media lifecycle
- `app/src/main/java/com/example/telemedicine_lab5/signaling/` - Socket signaling client and payload parser

## Quick Start

1. Run your Node signaling server from the lab guide on a machine reachable from Android devices.
2. Set the signaling URL in the app home screen (use LAN IP, not emulator localhost).
3. Start one client as doctor and another as patient in the same room.

## Build

```bash
cd /Users/ioan/AndroidStudioProjects/telemedicine_lab5
./gradlew :app:assembleDebug
```

## Unit Test

```bash
cd /Users/ioan/AndroidStudioProjects/telemedicine_lab5
./gradlew :app:testDebugUnitTest
```

## Flutter implementation (requested lab variant)

The repository now also contains a Flutter implementation in:

- `flutter_app/`

Included in the Flutter module:

- `flutter_webrtc` + `socket_io_client` dependencies
- Home screen (role, room, signaling URL)
- Call screen with offer/answer/ICE signaling
- Local PiP + remote fullscreen video
- Controls: mute, switch camera, hang up
- Android and iOS media permissions:
  - `flutter_app/android/app/src/main/AndroidManifest.xml`
  - `flutter_app/ios/Runner/Info.plist`

Run (when Flutter SDK is installed):

```bash
cd flutter_app
flutter pub get
flutter run
```
