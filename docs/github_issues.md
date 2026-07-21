# GitHub Issues for TL;DL Implementation

## Issue #1: [CORE] Transcription Service State Management & Orchestration
**Description:**
Implement a robust state management system for `TranscriptionService` using `StateFlow`. The service should act as the central orchestrator between `AudioProcessor` and `SpeechToTextEngine`.

**Tasks:**
- Define `TranscriptionState` (Idle, Decoding, Transcribing, Success, Error).
- Expose `StateFlow<TranscriptionState>` from `TranscriptionService`.
- Implement coroutine-based execution of the transcription pipeline.

---

## Issue #2: [AUDIO] MediaCodec Audio Decoding Implementation
**Description:**
Replace the current stub in `MediaCodecAudioDecoder` with a real implementation using Android `MediaExtractor` and `MediaCodec`.

**Tasks:**
- Implement PCM 16-bit 16kHz Mono conversion logic.
- Ensure proper resource management (releasing extractor/codec).
- Add unit tests for different audio formats.

---

## Issue #3: [UI] Activity-Service Integration
**Description:**
Connect `TransparentShareActivity` to `TranscriptionService`. Ensure the UI accurately reflects the service's state in real-time.

**Tasks:**
- Extract URI from `ACTION_SEND` intent.
- Start/Stop service based on user actions.
- Observe `TranscriptionService.state` from the Compose UI.

---

## Issue #4: [ENGINE] STT Engine Plumbing & Progress Loop
**Description:**
Refine `SherpaOnnxEngine` to handle the streaming feedback loop correctly and prepare it for JNI integration.

**Tasks:**
- Implement realistic chunk-based processing loop.
- Ensure `onProgress` callbacks are triggered correctly.
- Add error handling for initialization failures.
