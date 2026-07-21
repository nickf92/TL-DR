# Product Requirements Document (PRD) v2
**Progetto:** TL;DL - App di Trascrizione Vocale Offline e Privacy-First

## 1. Vision e Core Values
Un'applicazione Android open-source progettata per trascrivere messaggi vocali (da app di messaggistica come WhatsApp, Telegram, ecc.) interamente *on-device*. 
Il progetto si fonda su tre pilastri:
* **Privacy by Design:** Nessun dato audio o testuale lascia mai il dispositivo. Zero analytics, zero tracciamento.
* **Open Source:** Codice trasparente rilasciato sotto licenza **GNU GPLv3**, per proteggere il progetto da appropriazioni commerciali a codice chiuso.
* **Modularità AI:** Supporto per diverse famiglie di modelli STT (Speech-To-Text) per adattarsi all'hardware dell'utente.

## 2. Stack Tecnologico (MVP Android)
* **Linguaggio App:** Kotlin (Native Android).
* **Interfaccia Utente:** Jetpack Compose (MVVM).
* **Integrazione C/C++:** Android NDK tramite JNI.
* **Motore di Inferenza Base:** `sherpa-onnx` (permette di eseguire molteplici architetture come Whisper, SenseVoice, Moonshine usando un'unica libreria C++).
* **Conversione Audio:** `Android MediaCodec` nativo come prima scelta (hardware-accelerated, APK leggero) con fallback a `FFmpegKit` (versione audio-only) per formati/container non riconosciuti nativamente.

## 3. Gestione Modelli AI
* **Hosting:** I modelli verranno ospitati su un repository pubblico di **Hugging Face** gestito dal creatore dell'app.
* **Catalogo Modelli (JSON Manifest):** L'app includerà un manifest locale pre-incluso (`bundled_manifest.json`) nei file Asset per funzionare subito al 100% offline, e sincronizzerà l'elenco remoto da Hugging Face quando la rete è disponibile.
* **Onboarding "Smart Default":** Al primo avvio, l'app calcola la RAM totale e libera del dispositivo. In base a questo dato, propone automaticamente il compromesso ideale tra velocità e precisione. L'utente può accettare il suggerimento con un tap o accedere alla lista completa (con indicatori di sicurezza verdi) per cambiare modello.

## 4. Esperienza Utente (UX) e Flussi
### 4.1. Trascrizione
* **Innesco:** L'utente usa lo "Share Intent" (Condividi) dall'app di messaggistica.
* **Interfaccia:** L'app si apre come un **Bottom Sheet** su Activity trasparente in sovraimpressione sull'app originale (senza permessi speciali di overlay).
* **Esecuzione Lunga & Background:** Se l'audio è lungo o l'utente chiude la schermata, il processo di trascrizione continua tramite un **Foreground Service** con notifica di sistema e percentuale di avanzamento in tempo reale. Cliccando la notifica o riaprendo l'app a fine processo, si legge il testo.

### 4.2. Gestione della Cronologia
* **Memoria di Sessione (Default):** Finché il sistema non uccide l'app in background, i testi recenti restano in RAM. Questo evita di dover ritrascrivere lo stesso audio se l'utente lo ricondivide per sbaglio pochi secondi dopo.
* **Cronologia Locale (Opt-in Cifrato):** Nelle impostazioni è presente un'opzione (disattivata di default) per salvare in modo permanente le trascrizioni in un database `Room` locale cifrato con chiavi gestite dall'**Android KeyStore** (AES-256 / SQLCipher).

---
Per le specifiche tecniche dettagliate e l'architettura dei componenti, fare riferimento a [architecture_specs.md](file:///Users/nicola/Progetti/TL;DR/docs/architecture_specs.md).


### 4.3. UI e Design System
* **Material You:** L'interfaccia sfrutta i colori dinamici di Android, adattandosi allo sfondo dell'utente.
* **AMOLED Dark:** Supporto nativo e ottimizzato per il nero assoluto, per risparmiare batteria e offrire un look tecnico in modalità scura.
* **Azioni Rapide:** A fine trascrizione saranno presenti pulsanti per copiare il testo o ricondividerlo.

## 5. Architettura Software (High-Level)
Per garantire che in futuro sia facile aggiungere nuovi motori, il livello di dominio implementerà il pattern **Strategy**:

* `SpeechToTextEngine`: Interfaccia Kotlin base.
* `SherpaEngine` / `WhisperEngine`: Classi che implementano l'interfaccia e gestiscono i bridge JNI.
* Regola d'oro: **Un solo modello caricato in RAM alla volta**. Il caricamento di un nuovo modello forza l'invocazione di `release()` sul precedente per evitare crash di Out-Of-Memory (OOM).

## 6. Sviluppi Avanzati (Day 2 / Testing Build)
Le prime versioni includeranno un menu "Avanzate / Sperimentale" nelle impostazioni per testare e validare le seguenti funzionalità opzionali sul campo:

### 6.1. Voice Activity Detection (VAD)
Un sistema di filtraggio pre-elaborazione (es. Silero VAD) per identificare ed eliminare i segmenti di silenzio prima di passare l'audio al motore AI.
* **Controllo Utente:** Nelle impostazioni "Sperimentali" saranno presenti le opzioni di aggressività del VAD (Disattivato / Conservativo / Aggressivo) per testare l'impatto su velocità e qualità della punteggiatura.

### 6.2. Post-Processing Formattazione
Un modulo opzionale, separato dal motore principale, eseguito a trascrizione completata.
* **Scopo:** Integrare un modello AI specializzato, estremamente leggero (pochi MB), dedicato esclusivamente al ripristino avanzato della punteggiatura e delle maiuscole sul testo crudo generato dai modelli STT più rudimentali. Nessuna capacità generativa o di riassunto complessa.

## 7. Distribuzione e Sostenibilità
* **Distribuzione:** GitHub/GitLab Releases e **F-Droid** (richiede l'assenza totale di dipendenze proprietarie come Google Play Services).
* **Donazioni:** Nessuna monetizzazione forzata. Un pulsante discreto nel menu delle impostazioni permetterà donazioni volontarie tramite piattaforme open-friendly (es. Ko-fi, GitHub Sponsors o PayPal).
