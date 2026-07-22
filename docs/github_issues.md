# GitHub Issues Backlog per TL;DL

Questo documento contiene l'elenco e l'ordine di priorità di tutte le **issue aperte su GitHub** per lo sviluppo di TL;DL.

---

## 🔴 PRIORITÀ 1 — BUGFIX (Correzioni Critiche & Stabilità) [COMPLETATE]

### Issue #15: `fix: Gestione del ciclo di vita della RAM e rilascio risorse JNI nel motore STT` [CHIUSA]
- **URL:** [GitHub Issue #15](https://github.com/nickf92/TL-DR/issues/15)
- **Stato:** CHIUSA (Risolto)
- **Descrizione:** Invocare esplicitamente `sttEngine.release()` in `TranscriptionService` alla fine di ogni trascrizione e prima del cambio modello per liberare le risorse C++/JNI e prevenire Out-Of-Memory (OOM).

### Issue #16: `fix: Propagazione corretta delle eccezioni nel decoder audio FFmpeg` [CHIUSA]
- **URL:** [GitHub Issue #16](https://github.com/nickf92/TL-DR/issues/16)
- **Stato:** CHIUSA (Risolto)
- **Descrizione:** Modificare `FFmpegAudioDecoder` affinché lanci un'eccezione in caso di codice di ritorno non valido (`!ReturnCode.isSuccess`), consentendo ad `AudioProcessor` di rilevare e propagare gli errori anziché restituire buffer vuoti silenti.

---

## 🟡 PRIORITÀ 2 — CORE FEATURES (Architettura, Dati & Privacy)

### Issue #10: `feat: Lettura catalogo modelli da asset bundled_manifest.json e sync remoto`
- **URL:** [GitHub Issue #10](https://github.com/nickf92/TL-DR/issues/10)
- **Blocked by:** Nessuno (Inizio Immediato)
- **Descrizione:** Rimuovere la lista hardcoded dei modelli in Kotlin e implementare il caricamento del manifest dal file asset `bundled_manifest.json` offline, con sincronizzazione dinamica via HTTPS da Hugging Face quando la rete è disponibile.

### Issue #8: `feat: Cronologia locale cifrata con Room DB, SQLCipher e Android KeyStore` [CHIUSA]
- **URL:** [GitHub Issue #8](https://github.com/nickf92/TL-DR/issues/8)
- **Stato:** CHIUSA (Risolto)
- **Descrizione:** Configurare Room DB con cifratura SQLCipher (AES-256) e chiavi gestite dall'Android KeyStore, connettere `TranscriptionService` al salvataggio opt-in e aggiungere il toggle e la vista della cronologia nelle Impostazioni.

### Issue #12: `feat: Aggiungere pulsante 'Annulla' alla notifica di trascrizione in corso` [CHIUSA]
- **URL:** [GitHub Issue #12](https://github.com/nickf92/TL-DR/issues/12)
- **Stato:** CHIUSA (Risolto)
- **Descrizione:** Aggiungere l'azione `ACTION_CANCEL` alla notifica `ForegroundService` per consentire all'utente di interrompere un'elaborazione in corso dalla tendina delle notifiche.

---

## 🟢 PRIORITÀ 3 — UI & ESPERIENZA UTENTE [COMPLETATE]

### Issue #17: `feat: Badge visivi di sicurezza RAM e Smart Default nella UI Impostazioni` [CHIUSA]
- **URL:** [GitHub Issue #17](https://github.com/nickf92/TL-DR/issues/17)
- **Stato:** CHIUSA (Risolto con TDD)
- **Descrizione:** Calcolare la RAM libera attuale del dispositivo e mostrare un badge visivo verde sui modelli idonei, evidenziando il modello consigliato (*Smart Default*).

### Issue #11: `feat: Player audio per la riproduzione dell'audio originale nel BottomSheet` [CHIUSA]
- **URL:** [GitHub Issue #11](https://github.com/nickf92/TL-DR/issues/11)
- **Stato:** CHIUSA (Risolto con TDD)
- **Descrizione:** Aggiungere la riproduzione dell'audio originale (tramite MediaPlayer nativo o ExoPlayer) tra le azioni rapide del `TranscriptionBottomSheet`.

### Issue #18: `feat: Supporto AMOLED Dark Mode e Dynamic Colors (Material You)` [CHIUSA]
- **URL:** [GitHub Issue #18](https://github.com/nickf92/TL-DR/issues/18)
- **Stato:** CHIUSA (Risolto con TDD)
- **Descrizione:** Implementare il supporto completo ai colori dinamici Material You su Android 12+ e offrire un tema scuro ottimizzato a nero assoluto (#000000) per schermi AMOLED.

---

## 🔵 PRIORITÀ 4 — AVANZATE & OPZIONALI

### Issue #9: `feat: Integrazione Voice Activity Detection (Silero VAD) per filtraggio del silenzio`
- **URL:** [GitHub Issue #9](https://github.com/nickf92/TL-DR/issues/9)
- **Blocked by:** Issue #2 (Chiusa)
- **Descrizione:** Integrare il modulo di pre-elaborazione VAD per eliminare le pause di silenzio e aggiungere il selettore di aggressività nelle impostazioni "Sperimentali".

### Issue #13: `feat: Aggiungere sezione donazioni volontarie (Ko-fi / GitHub Sponsors / PayPal)`
- **URL:** [GitHub Issue #13](https://github.com/nickf92/TL-DR/issues/13)
- **Blocked by:** Nessuno
- **Descrizione:** Aggiungere una sezione discreta nella schermata impostazioni con link esterni per le donazioni volontarie.
