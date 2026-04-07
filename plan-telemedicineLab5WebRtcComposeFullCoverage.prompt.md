## Plan: Full Lab5 WebRTC Coverage

Translate all requirements from the lab guide into a native Android Compose plan, while preserving your requested architecture (single `MainActivity`, NavHost navigation, UI/business-logic separation). This plan now includes full signaling/media flow, PiP and doctor controls, permission/runtime handling, state/coroutine patterns, troubleshooting expectations, and non-functional compliance notes so nothing from the guide is missed.

### Steps
1. Create a requirement matrix from [/Users/ioan/Downloads/L5 - consultație video.txt](/Users/ioan/Downloads/L5 - consultație video.txt) and map each item to app components (`UI`, `WebRTC`, `signaling`, `config`, `docs`).
2. Keep architecture strict: minimal `MainActivity` host, route-based `NavHost`, and package split across [app/src/main/java/com/example/telemedicine_lab5/MainActivity.kt](app/src/main/java/com/example/telemedicine_lab5/MainActivity.kt), [app/src/main/java/com/example/telemedicine_lab5/ui/screens/](app/src/main/java/com/example/telemedicine_lab5/ui/screens/), [app/src/main/java/com/example/telemedicine_lab5/ui/navigation/](app/src/main/java/com/example/telemedicine_lab5/ui/navigation/), [app/src/main/java/com/example/telemedicine_lab5/webrtc/](app/src/main/java/com/example/telemedicine_lab5/webrtc/), and [app/src/main/java/com/example/telemedicine_lab5/signaling/](app/src/main/java/com/example/telemedicine_lab5/signaling/).
3. Cover platform setup completely in [app/build.gradle.kts](app/build.gradle.kts), [gradle/libs.versions.toml](gradle/libs.versions.toml), and [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml): WebRTC + socket + navigation dependencies, required permissions, and runtime permission flow via `rememberLauncherForActivityResult`.
4. Implement full call lifecycle contract in `WebRtcSessionManager` and `SocketSignalingClient`: media init/dispose, STUN config, `RTCPeerConnection`, add local tracks, `onTrack`, `onIceCandidate`, `join`, `message`, `offer`, `answer`, `candidate` (`candidate`, `sdpMid`, `sdpMLineIndex`) aligned to Node server forwarding.
5. Implement full telemedicine UI contract in `CallScreen`: start button sequence (`openUserMedia -> createPeerConnection -> makeCall`), full-screen remote video, mirrored local PiP overlay, doctor control bar (`mute`, `hangUp`, `switchCamera`), and reactive state (`Connecting`, `Connected`, `Failed`) with coroutine-backed actions.

### Further Considerations
1. Role model decision: Option A auto by room occupancy / Option B explicit `Doctor`-`Patient` route / Option C both.
2. Minimum scope decision: Option A grading-only mandatory items / Option B grading + full troubleshooting UX / Option C grading + compliance-ready metadata hooks.
3. Draft review checkpoint: confirm this “include everything” scope before converting into a final execution-ready implementation task list.

