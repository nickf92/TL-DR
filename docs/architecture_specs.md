# Specifiche Tecniche e Architetturali (Rifinite)
**Progetto:** TL;DR - App di Trascrizione Vocale Offline e Privacy-First per Android

## 1. Sintesi delle Decisioni Architetturali (Grill-Me)

| Ambito | Scelta Architetturale | Rationale & Benefici |
| :--- | :--- | :--- |
| **Background Execution** | `Foreground Service` con notifica persistente e progress bar | Garantisce che l'elaborazione non venga terminata da Android durante l'uso di altre app e aggiorna in tempo reale la percentuale di completamento. |
| **Audio Pipeline & Decodifica** | `Android MediaCodec` (Primary) + `FFmpegKit audio-only` (Fallback) | Riduce la dimensione dell'APK di ~10-15MB sfruttando i decodificatori hardware nativi di Android (Opus, AAC, MP3), usando FFmpeg solo per formati non riconosciuti. |
| **Gestione Catalogo Modelli** | `Asset locale (bundled_manifest.json)` + Sync da Hugging Face | Funzionamento 100% offline out-of-the-box con aggiornamento remoto delle ultime novità quando la rete è disponibile. |
| **UX & Trigger Share Intent** | `Bottom Sheet su Activity Trasparente` | Interfaccia in sovraimpressione diretta sulle app di messaggistica (WhatsApp, Telegram) senza richiedere permessi speciali di overlay (`SYSTEM_ALERT_WINDOW`). |
| **Persistenza & Privacy** | `Room DB Cifrato` (Android KeyStore + AES-256/SQLCipher) | Cronologia opt-in criptata nativamente con chiavi generate in hardware safe-storage; di default vige la sola memoria RAM temporanea. |

---

## 2. Dettaglio Componenti e Flussi di Sistema

### 2.1. Pipeline Audio & Conversione (`AudioProcessor`)
1. **Ricezione File:** L'utente invia un file audio (es. `.opus` da WhatsApp, `.m4a` da Telegram, `.mp3`, `.wav`) all'app tramite `ACTION_SEND` (Share Intent).
2. **Estrazione e Parsing:** `MediaExtractor` analizza il contenitore e le tracce audio.
3. **Decodifica Hardware (`MediaCodec`):** L'audio viene decodificato in streaming PCM raw.
4. **Resampling & Downmixing:** I campioni audio vengono convertiti in **PCM 16-bit Mono @ 16000 Hz** (formato standard richiesto dai modelli STT).
5. **Fallback Strategy:** Se `MediaCodec` restituisce un errore di parsing/decodifica, l'operazione viene delegata a `FFmpegKit` per una conversione via codice C/C++.

---

### 2.2. Motore STT (`sherpa-onnx` JNI Integration)
* **Pattern Strategy:** Interfaccia `SpeechToTextEngine` con classe concreta `SherpaOnnxEngine`.
* **Approccio Streaming (Chunk-based):** 
  * Il file PCM convertito viene passato al motore `sherpa-onnx` a blocchi (chunk di buffer audio).
  * Consente l'elaborazione progressiva e riduce il picco di allocazione RAM.
  * Fornisce feedback temporali frequenti per la barra di progresso nella notifica e nella UI.
* **Gestione Memoria RAM (Single-Model Constraint):**
  * Un solo modello caricato in memoria C++/JNI alla volta.
  * Invocazione esplicita di `release()` / `destroy()` prima del cambio modello per prevenire Out-Of-Memory (OOM).
  * Il calcolo della RAM libera del dispositivo assegna un badge visivo **Verde** ai modelli compatibili nel catalogo.

---

### 2.3. Trascrizione in Background (`TranscriptionService`)
* **Service Type:** `ForegroundService` (con tipo `shortService` o `dataSync` in base all'API level Android).
* **Notifica di Sistema:**
  * Mostra titolo "Trascrizione in corso...", percentuale avanzamento (0-100%) e pulsante "Annulla".
  * A completamento, la notifica si aggiorna in "Trascrizione completata" con azione rapida "Copia testo" o "Apri".
* **Scollegamento dalla UI:** Se la `BottomSheet` viene chiusa, il `TranscriptionService` continua la sua esecuzione senza perdite di stato.

---

### 2.4. Interfaccia Utente (Jetpack Compose & Material 3)
* **Theme:** Dynamic Colors (Material You) + AMOLED Dark Mode.
* **Activity:** `TransparentShareActivity` (tema `Theme.Transparent` / `Theme.Material3.DayNight.NoActionBar`).
* **Componenti UI principale:** `ModalBottomSheetLayout` con:
  * Anteprima stato (Loading / Progress Bar / Risultato).
  * Pulsanti d'azione rapida: *Copia negli appunti*, *Ricondividi testo*, *Riproduci audio original*.
  * Suggerimento modello iniziale basato su RAM libera (Onboarding "Smart Default").

---

### 2.5. Storage & Privacy Layer (`HistoryRepository`)
* **Default (RAM Cache):** `SessionMemoryStore` conserva gli ultimi testi trascritti solo in RAM finché il processo resta in vita.
* **Opt-In (Local History):**
  * Interruttore nelle Impostazioni: "Salva cronologia locale".
  * Se abilitato, i dati vengono persistiti in `AppDatabase` (Room) cifrato con **SQLCipher** / **Encrypted DB** tramite chiavi gestite dall'**Android KeyStore**.
  * Zero invio di dati all'esterno; zero analytics.

---

## 3. Prossimi Passi per lo Sviluppo
1. Configurazione progetto Android Kotlin / Gradle con modulo NDK e dipendenze `sherpa-onnx` / `FFmpegKit`.
2. Implementazione `MediaCodecAudioDecoder` con fallback FFmpeg.
3. Creazione del bridge JNI Kotlin-C++ per `sherpa-onnx`.
4. Sviluppo di `TranscriptionService` (Foreground Service) e `TransparentShareActivity` (Compose Bottom Sheet).
5. Costruzione del catalogo modelli JSON e logica di calcolo RAM.
