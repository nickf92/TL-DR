# Specifica Tecnica: Pipeline di Decodifica Audio (`AudioProcessor`)

**Modulo:** `it.tldl.app.core.audio`  
**Data:** 22 Luglio 2026  
**Stato:** Approvato

---

## 1. Obiettivo ed Architettura Generale

Il modulo `AudioProcessor` ha la responsabilità di convertire qualsiasi file audio o messaggio vocale in ingresso (proveniente dallo Share Intent di WhatsApp, Telegram, Signal o altre app) in un formato **PCM raw unificato a 16000 Hz, Mono, 16-bit Little-Endian**, ovvero il formato standard richiesto dal motore STT `sherpa-onnx`.

La decodifica adotta il pattern **Strategy con Fallback Ibrido**:
1. **Decodificatore Primario (`MediaCodecAudioDecoder`):** Sfrutta gli estrattori e i codec hardware nativi del sistema operativo Android (`MediaExtractor` e `MediaCodec`). Riduce le dimensioni dell'APK e il consumo energetico.
2. **Decodificatore di Fallback (`FFmpegAudioDecoder`):** Interviene qualora `MediaCodec` fallisca la decodifica (es. per contenitori o codec non riconosciuti nativamente da Android). Sfrutta la libreria C++ `FFmpegKit` (versione audio-only).

---

## 2. Diagramma del Flusso di Decodifica

```
                 +--------------------------------+
                 | Messaggio Audio (URI / Stream) |
                 +---------------+----------------+
                                 |
                                 v
                 +---------------+----------------+
                 |     AudioProcessor.decode()    |
                 +---------------+----------------+
                                 |
                        (Tentativo Primario)
                                 v
                +----------------------------------+
                | MediaCodecAudioDecoder.decode()  |
                +----------------+-----------------+
                                 |
                   +-------------+-------------+
                   |                           |
            (Esito OK)                    (Eccezione /
                   |                       Errore Format)
                   v                           |
         +------------------+                  v
         | ByteArray (PCM)  |     +----------------------------+
         +------------------+     | FFmpegAudioDecoder.decode()|
                                  +--------------+-------------+
                                                 |
                                     +-----------+-----------+
                                     |                       |
                              (Esito OK)               (Eccezione)
                                     |                       |
                                     v                       v
                           +------------------+     +-------------------+
                           | ByteArray (PCM)  |     | Propaga Eccezione |
                           +------------------+     +-------------------+
```

---

## 3. Matrice dei Formati Audio Supportati

| Formato Audio / Contenitore | Estensione Tipica | Decodificatore Primario (`MediaCodec`) | Fallback (`FFmpegKit`) | Note |
| :--- | :--- | :--- | :--- | :--- |
| **Opus (Ogg container)** | `.opus`, `.ogg` | Native Android 10+ | Supportato | Formato standard dei vocali **WhatsApp** |
| **AAC / M4A** | `.m4a`, `.aac` | Hardware-accelerated | Supportato | Formato standard dei vocali **Telegram** |
| **MP3** | `.mp3` | Hardware-accelerated | Supportato | Audio generico |
| **FLAC** | `.flac` | Lossless native | Supportato | Audio generico ad alta fedeltà |
| **AMR-NB / AMR-WB** | `.amr` | Supported | Supportato | Registrazioni telefoniche o vocali vecchi |
| **WAV** | `.wav` | PCM Direct Parsing | Supportato | Ripristino diretto header WAV PCM |

---

## 4. Normalizzazione e Risoluzione dei Parametri Audio

Ogni traccia audio decodificata deve soddisfare i seguenti parametri di output:

- **Sample Rate:** `16000 Hz` (Resampling effettuato in-flight se la traccia originale è a 44100 Hz o 48000 Hz).
- **Canali:** `1 (Mono)` (Downmixing effettuato calcolando la media dei canali Stereo L+R: $(L + R) / 2$).
- **Encoding:** `AudioFormat.ENCODING_PCM_16BIT` (2 byte per campione, Little-Endian).

---

## 5. Gestione delle Eccezioni e Sicurezza

1. **Rilevamento Fallimento Silente:** `FFmpegAudioDecoder` verifica il codice di ritorno del processo C++. Se il ReturnCode è `!isSuccess`, viene lanciata un'eccezione esplicita anziché restituire un buffer vuoto di 0 byte (Issue #16).
2. **Cleanup delle Risorse:** Entrambi i decoder chiudono ed eliminano esplicitamente `MediaExtractor`, `MediaCodec` e i file temporanei decodificati al termine dell'operazione.
