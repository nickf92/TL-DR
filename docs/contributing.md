# Guida al Contributo e Sviluppo Locale per TL;DL 🚀

Grazie per l'interesse a contribuire a **TL;DL**! Questa guida fornisce tutte le informazioni necessarie per configurare l'ambiente di sviluppo locale, comprendere l'architettura del codice, eseguire la suite di test e inviare contributi al progetto.

---

## 🛠️ 1. Requisiti e Setup dell'Ambiente

Per compilare ed eseguire TL;DL sul tuo sistema locale sono necessari:

* **Android Studio:** Ladybug (2024.2.1) o versione più recente.
* **JDK:** Java Development Kit version 17 (configurato come Gradle JDK).
* **Android SDK:** API Level 34 (Android 14) come Target/Compile SDK, API 26 (Android 8.0) come Min SDK.
* **Android NDK & CMake:** NDK (versione 26.x o superiore) per la compilazione dei moduli C++/JNI nativi di `sherpa-onnx` e `FFmpegKit`.

### Procedura di Clonazione e Setup

```bash
# 1. Clona il repository
git clone https://github.com/nickf92/TL-DR.git
cd TL-DR

# 2. Verifica la compilazione del progetto
./gradlew assembleDebug

# 3. Esegui la suite di test unitari
./gradlew test
```

---

## 🏗️ 2. Architettura del Codice e Convenzioni

TL;DL è strutturato seguendo l'architettura **MVVM (Model-View-ViewModel)** pulita e modulare con il pattern **Strategy** per i componenti intercambiabili.

### Struttura dei Package (`app/src/main/java/it/tldl/app/`)

```
it.tldl.app/
├── MainActivity.kt               # Entrypoint UI principale dell'applicazione
├── TransparentShareActivity.kt   # Activity trasparente attivata dallo Share Intent
├── core/
│   ├── audio/                    # Pipeline decodifica audio (MediaCodec, FFmpegKit fallback)
│   ├── database/                 # Persistenza cifrata (Room, SQLCipher, KeyStore)
│   ├── service/                  # ForegroundService per elaborazione in background
│   └── stt/                      # Motore STT (sherpa-onnx JNI, RamCalculator, Tokenizer)
└── ui/
    ├── SettingsViewModel.kt      # ViewModel per le impostazioni e lo stato dei modelli
    ├── TranscriptionBottomSheet.kt # Interfaccia Compose in sovraimpressione
    └── theme/                    # Tema Material 3, Dynamic Colors e AMOLED Dark
```

### Pattern e Regole Architetturali

1. **Simplicità & Surgical Changes:** Nessun grado di astrazione prematuro per codice a singolo uso.
2. **Strategy Pattern per STT e Decodifica Audio:**
   - `SpeechToTextEngine`: Interfaccia Kotlin base per motori STT.
   - `AudioDecoder`: Interfaccia Kotlin per decodificatori audio (`MediaCodecAudioDecoder`, `FFmpegAudioDecoder`).
3. **Single-Model RAM Constraint:**
   - Un solo modello STT caricato in memoria nativa C++/JNI alla volta.
   - È obbligatorio chiamare `release()` / `destroy()` sul motore precedente prima di caricare un nuovo modello per prevenire Out-Of-Memory (OOM).

---

## 🧪 3. Suite di Test e Filosofia TDD

Il progetto adotta un approccio **Test-Driven Development (TDD)** per garantire che la logica di dominio, la decodifica audio e la gestione della RAM siano sempre verificate empiricamente.

### Organizzazione dei Test (`app/src/test/java/it/tldl/app/`)

- **`AudioProcessorTest.kt`**: Verifica la logica di fallback tra decodificatore hardware `MediaCodec` e `FFmpegKit`.
- **`MediaCodecAudioDecoderTest.kt`**: Test di estrazione e conversion PCM mono @ 16kHz.
- **`RamCalculatorTest.kt`**: Verifica l'algoritmo di raccomandazione del modello e assegnazione dei badge visivi verdi in base alla RAM libera.
- **`TranscriptionServiceTest.kt`**: Test del ciclo di vita del Foreground Service e notifica.
- **`HistoryRepositoryTest.kt`**: Verifica del salvataggio opt-in su Room DB cifrato.
- **`TranscriptionUiStateTest.kt` / `ModelItemStateTest.kt` / `ThemeColorSchemeTest.kt`**: Test dello stato UI Compose e temi Material You / AMOLED.

### Esecuzione dei Test

Per eseguire tutti i test unitari locali via CLI:

```bash
./gradlew testDebugUnitTest
```

---

## 📋 4. Guida per l'Invio di Pull Request (PR)

1. **Fork & Branching:**
   - Crea un feature branch partendo da `main`: `git checkout -b feature/nome-funzionalita` o `fix/nome-bug`.
2. **Qualità del Codice:**
   - Assicurati che tutti i test unitari passino prima di creare il commit: `./gradlew testDebugUnitTest`.
   - Mantieni modifiche chirurgiche: non rifattorizzare codice non correlato alla PR.
3. **Messaggi di Commit:**
   - Utilizza i Conventional Commits: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`.
4. **Apri la Pull Request:**
   - Descrivi chiaramente cosa risolve la PR e collega l'Issue GitHub corrispondente (es. `Closes #10`).
