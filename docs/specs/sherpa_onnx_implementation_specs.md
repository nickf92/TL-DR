# Specifiche di Implementazione: Integrazione Sherpa-ONNX e Gestione Modelli

## 1. Architettura JNI e Gestione Trascrizione
La gestione del motore Sherpa-ONNX sarà centralizzata in una classe Kotlin dedicata, incaricata di interfacciarsi con la libreria C++ nativa tramite JNI.

**Firme e Metodi Principali (Bridge JNI)**
*   **`initialize(modelPath: String)`:** Inizializza l'istanza del motore caricando i file del modello dal percorso specificato.
*   **`destroy()`:** Si occupa di liberare la memoria e chiudere la sessione in modo sicuro al termine delle operazioni.

**Logica di Elaborazione Audio**
*   **Approccio Unificato (Streaming):** L'applicazione implementerà e utilizzerà *esclusivamente* la modalità di trascrizione in streaming (a blocchi).
*   **Trascrizione File Interi:** L'elaborazione di registrazioni audio già concluse avverrà passando i dati a chunk al metodo di streaming. Questo avviene in modo completamente trasparente per l'utente e permette di mantenere una singola pipeline logica nel codice.
*   **Esecuzione in Background:** Il processo di elaborazione audio sarà demandato a un servizio in background scollegato dal ciclo di vita della UI (es. Bottom Sheet). Se l'utente chiude la finestra durante un'elaborazione lunga, il servizio continuerà a processare i blocchi audio successivi senza perdere il lavoro già fatto, per poi notificare il completamento.

---

## 2. Logica di Suggerimento Modelli e Calcolo RAM
Il sistema di selezione del modello ottimizzerà le risorse del dispositivo bilanciando le prestazioni di trascrizione con il consumo di memoria (RAM libera).

**Criteri di Selezione Automatica**
*   **Modello Ideale (Cap):** Verrà individuato in fase di test un modello "medio-piccolo" con un rapporto eccellente tra prestazioni e peso in memoria. Questo modello rappresenterà la **soglia massima** per i suggerimenti automatici. Anche sui dispositivi top di gamma con grandi quantità di RAM libera, l'app consiglierà questo modello ideale per evitare sprechi inutili di memoria.
*   **Scalabilità (Fallback):** In caso di dispositivi con RAM libera ridotta, l'algoritmo scalerà il suggerimento verso modelli progressivamente più piccoli, garantendo il caricamento sicuro a discapito di una marginale perdita di precisione.

**Interfaccia Utente (UI) e Libertà di Scelta**
*   **Indicatori Visivi:** La lista dei modelli scaricabili/utilizzabili presenterà un feedback visivo (es. testo o icona di colore **verde**) per indicare chiaramente quali modelli possono essere caricati in sicurezza in base alla RAM libera *attuale* del dispositivo.
*   **Override dell'Utente:** L'utente avrà sempre l'ultima parola. Potrà ignorare il modello suggerito dall'app e selezionarne manualmente uno più grande, a patto che l'indicatore visivo confermi che il modello scelto rientra nei limiti di sicurezza della memoria del telefono, evitando così crash per *Out Of Memory*.