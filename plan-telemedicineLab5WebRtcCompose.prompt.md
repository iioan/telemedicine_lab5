## Plan: Compose WebRTC Teleconsultation Architecture

Build a native Android (Jetpack Compose) version of the lab flow while preserving your requested architecture: single `MainActivity`, NavHost-based screen routing, UI/business-logic separation, modern permission launchers, coroutine-backed async work, and reactive local state. The plan introduces a minimal navigation graph (`home` + `call`) and isolates signaling/WebRTC logic into non-UI classes so screens stay focused on rendering and user actions.

### Steps
1. Add Android-native WebRTC/navigation/signaling dependencies in [app/build.gradle.kts](app/build.gradle.kts) and [gradle/libs.versions.toml](gradle/libs.versions.toml) (`navigation-compose`, `socket.io-client`, `google-webrtc`).
2. Declare media/network permissions in [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) for `CAMERA`, `RECORD_AUDIO`, `INTERNET`, and `MODIFY_AUDIO_SETTINGS`.
3. Refactor `MainActivity` in [app/src/main/java/com/example/telemedicine_lab5/MainActivity.kt](app/src/main/java/com/example/telemedicine_lab5/MainActivity.kt) to only host theme + `NavHost` (`rememberNavController`).
4. Create navigation and screens in [app/src/main/java/com/example/telemedicine_lab5/ui/navigation/AppNavGraph.kt](app/src/main/java/com/example/telemedicine_lab5/ui/navigation/AppNavGraph.kt), [app/src/main/java/com/example/telemedicine_lab5/ui/screens/HomeScreen.kt](app/src/main/java/com/example/telemedicine_lab5/ui/screens/HomeScreen.kt), and [app/src/main/java/com/example/telemedicine_lab5/ui/screens/CallScreen.kt](app/src/main/java/com/example/telemedicine_lab5/ui/screens/CallScreen.kt).
5. Implement business logic classes outside `ui` (e.g., [app/src/main/java/com/example/telemedicine_lab5/webrtc/WebRtcSessionManager.kt](app/src/main/java/com/example/telemedicine_lab5/webrtc/WebRtcSessionManager.kt), [app/src/main/java/com/example/telemedicine_lab5/signaling/SocketSignalingClient.kt](app/src/main/java/com/example/telemedicine_lab5/signaling/SocketSignalingClient.kt)) for SDP/ICE exchange and peer lifecycle.
6. Wire `rememberLauncherForActivityResult`, `rememberCoroutineScope`, and `mutableStateOf` in `CallScreen` for permission flow, start-call sequence, PiP layout, and controls (`mute`, `switchCamera`, `hangUp`) with cleanup on dispose.

### Further Considerations
1. Confirm call role flow: Option A single “Start Consultation”, Option B explicit “Doctor/Patient”, Option C auto-role by room occupancy.
2. Confirm signaling payload contract with your existing Node server (`join`, `message`, room field, payload structure) before wiring handlers.
3. Decide state-holder style: Option A local screen state only, Option B `ViewModel` for better lifecycle resilience.

