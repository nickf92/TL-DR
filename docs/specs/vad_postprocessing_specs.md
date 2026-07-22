# Specifica Tecnica: Pre-Processing VAD e Post-Processing Formattazione

**Modulo:** `it.tldl.app.core.stt`  
**Data:** 22 Luglio 2026  
**Stato:** Approvato (Preparazione Issue #9)

---

## 1. Introduzione e Motivazione

Nei contesti di trascrizione vocale *on-device*, i messaggi audio reali presentano spesso periodi prolungati di silenzio, rumore di fondo o esitazioni della voce. 
Inoltre, i modelli STT più leggeri possono produrre testo "crudo" privo di maiuscole appropriate o punteggiatura corretta.

Questa specifica descrive l'architettura dei moduli di **Pre-Processing (Voice Activity Detection)** e **Post-Processing (Text Formatting)** integrati nel `TranscriptionPipeline`.

---

## 2. Architettura della Pipeline Completa

```
[Audio File (OPUS/M4A)] 
           │
           ▼
┌───────────────────────┐
│     AudioProcessor    │ ───► Converte in PCM 16kHz Mono 16-bit
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│   Silero VAD (Opt)    │ ───► Rimuove i blocchi PCM di silenzio
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│ SpeechToTextEngine    │ ───► Genera il testo crudo (Raw Text)
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│ TextCleanerEngine     │ ───► Applica regex/regole di formattazione,
└──────────┬────────────┘      punteggiatura e maiuscole
           │
           ▼
[Testo Trascritti & Formattato]
```

---

## 3. Pre-Processing: Voice Activity Detection (VAD)

L'integrazione di **Silero VAD** (o motore VAD compatibile C++/ONNX) permette di filtrare i campioni audio prima di passarli a `sherpa-onnx`.

### Vantaggi Prestazionali
- **Velocità:** Salta la decodifica STT (molto onerosa in termini di calcolo) sui segmenti di silenzio.
- **Accuratezza:** Riduce le allucinazioni del testo (comuni nei modelli Whisper su tracce audio silenziose).

### Livelli di Aggressività nelle Impostazioni Sperimentali

1. **Disattivato (Default):** Passa l'intero stream PCM a `sherpa-onnx`.
2. **Conservativo:** Rimuove i segmenti di silenzio superiori a `750 ms`.
3. **Aggressivo:** Rimuove i segmenti di silenzio superiori a `300 ms` e applica un filtro sui rumori ad alta frequenza.

---

## 4. Post-Processing: Formattazione del Testo (`TextCleanerEngine`)

Il modulo `TextCleanerEngine` elabora il testo generato da `SpeechToTextEngine` applicando una serie di trasformatori regolari e deterministici:

1. **Ripristino Maiuscole (`CapitalizationTransformer`):**
   - Garantisce la maiuscola all'inizio di ogni frase (dopo `.`, `?`, `!`).
2. **Normalizzazione Spazi e Punteggiatura (`PunctuationCleaner`):**
   - Rimuove spazi multipli prima della punteggiatura (`ciao , come stai ?` -> `ciao, come stai?`).
   - Normalizza punti sospensivi (`...`).
3. **Rimozione Filler Words (Opzionale):**
   - Filtraggio di esitazioni comuni (es. "ehm", "mmm", "ah").

---

## 5. Requisiti di Sicurezza e Prestazioni

- **Esecuzione In-Memory:** Sia il VAD che il `TextCleanerEngine` operano interamente in RAM senza allocazioni persistenti su disco.
- **Isolamento Moduli:** Qualsiasi eccezione sollevata da `TextCleanerEngine` o VAD viene catturata in sicurezza, restituendo il testo crudo generato dal motore STT senza interrompere l'esperienza utente.
