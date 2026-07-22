# TL;DL (Too Long; Didn't Listen) 🎙️⚡

**TL;DL** è un'applicazione Android open-source e privacy-first progettata per la **trascrizione vocale interamente on-device** dei messaggi audio ricevuti da app di messaggistica come WhatsApp, Telegram e Signal.

Nessun dato audio o testuale viene mai inviato a server remoti: l'elaborazione avviene al 100% sul tuo dispositivo in modo sicuro, veloce e privato.

---

## 💡 Idea del Progetto

I messaggi vocali nelle app di messaggistica sono comodi da inviare, ma spesso sconvenienti o lunghi da ascoltare. Le soluzioni di trascrizione esistenti si affidano a servizi cloud esterni, ponendo seri problemi di privacy e sicurezza per i dati personali dell'utente.

**TL;DL** nasce per risolvere questo problema fondandosi su tre pilastri cardine:

1. **Privacy by Design:** Elaborazione audio e riconoscimento vocale completati interamente *on-device*. Zero tracciamento, zero analytics, zero trasmissione di dati esterni.
2. **Open Source:** Codice totalmente trasparente rilasciato sotto licenza **GNU GPLv3**, a garanzia della sicurezza e della tutela della community.
3. **Modularità AI & Prestazioni Scalabili:** Supporto per architetture Speech-To-Text (STT) come Whisper, SenseVoice e Moonshine tramite il motore `sherpa-onnx`. L'app seleziona e raccomanda il modello più adatto in base alle risorse RAM hardware del dispositivo.

---

## ✨ Funzionalità Principali

- 🔒 **100% Offline & Riservato:** Nessun utilizzo di API esterne per la trascrizione. Funziona perfettamente anche senza connessione internet.
- 📲 **Integrazione "Share Intent" Intuitiva:** Condividi qualsiasi messaggio vocale da WhatsApp o Telegram verso TL;DL: si aprirà una schermata in sovraimpressione (*Bottom Sheet*) rapida e trasparente, senza dover cambiare contesto.
- ⚙️ **Foreground Service per File Lunghi:** Per gli audio più lunghi, l'elaborazione continua in background tramite notifica di sistema con percentuale di completamento in tempo reale.
- 🧠 **Onboarding & Gestione Modelli "Smart Default":** Calcolo automatico della RAM libera all'avvio e raccomandazione del modello STT ideale per bilanciare velocità e accuratezza. Sincronizzazione dinamica del catalogo dei modelli da Hugging Face con fallback offline pre-incluso.
- 🎵 **Pipeline Audio Adattiva:** Decodifica hardware ad alte prestazioni con `Android MediaCodec` (Opus, AAC, MP3) per minimizzare le dimensioni dell'APK e l'uso di batteria, con fallback automatico a `FFmpegKit audio-only` per contenitori audio insoliti.
- 🔐 **Persistenza & Cronologia Opt-in Cifrata:** Di default le trascrizioni risiedono solo nella memoria volatile (RAM). È possibile abilitare la cronologia locale cifrata con **Room**, **SQLCipher** e chiavi hardware via **Android KeyStore (AES-256)**.
- 🎨 **Material You & AMOLED Dark Mode:** Interfaccia moderna con colori dinamici sincronizzati con lo sfondo del sistema e modalità scura a contrasto elevato per schermi OLED.
- 🛠️ **Strumenti Avanzati (Sperimentali):** Integrazione opzionale con Voice Activity Detection (Silero VAD) per eliminare i periodi di silenzio e post-processing per il ripristino di punteggiatura e maiuscole.

---

## 🛠️ Stack Tecnologico

- **Linguaggio App:** Kotlin (Native Android)
- **UI Framework:** Jetpack Compose (MVVM Architecture)
- **C/C++ Integration:** Android NDK tramite JNI
- **Motore STT:** `sherpa-onnx` (ONNX Runtime)
- **Decodifica Audio:** `MediaCodec` nativo + `FFmpegKit` (audio-only fallback)
- **Database Cifrato:** Room DB + SQLCipher + Android KeyStore

---

## 🚀 Guida all'Uso

### 1. Come trascrivere un messaggio vocale

1. Apri **WhatsApp**, **Telegram** o qualunque app di messaggistica.
2. Seleziona il messaggio vocale che desideri trascrivere.
3. Tocca il pulsante **Condividi** (Share).
4. Seleziona **TL;DL** dall'elenco delle applicazioni.
5. Il *Bottom Sheet* di TL;DL apparirà in sovraimpressione mostrando la trascrizione in tempo reale.
6. A fine processo puoi **copiare il testo negli appunti** o **ricondividerlo** con un solo tap.

### 2. Gestione e Download Modelli STT

Al primo avvio, l'app ti suggerirà il modello migliore per la RAM del tuo dispositivo. Puoi modificare il modello in uso o scaricarne altri in qualsiasi momento:
1. Apri l'app **TL;DL** dal drawer delle applicazioni.
2. Vai su **Impostazioni** > **Gestione Modelli AI**.
3. Consulta il catalogo e seleziona il modello STT preferito (es. Whisper, SenseVoice).

---

## 💻 Sviluppo e Build Locale

### Requisiti
- Android Studio Ladybug (o versione più recente)
- JDK 17+
- Android SDK (API Level 26+)
- Android NDK (con Cmake per l'integrazione C++)

### Compilazione via CLI

Per clonare il repository e compilare l'APK di debug:

```bash
git clone https://github.com/tuo-username/TL-DL.git
cd TL-DL
./gradlew assembleDebug
```

L'APK compilato sarà disponibile nella directory:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📖 Documentazione Tecnica

Per approfondire l'architettura e i requisiti di sistema:
- [Product Requirements Document (PRD)](docs/PRD.md)
- [Specifiche Tecniche e Architetturali](docs/architecture_specs.md)

---

## 📜 Licenza

Questo progetto è distribuito sotto licenza **GNU General Public License v3.0 (GPLv3)**. Consulta il file [LICENSE](LICENSE) per maggiori informazioni.
