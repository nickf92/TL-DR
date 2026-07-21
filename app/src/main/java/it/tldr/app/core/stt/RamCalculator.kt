package it.tldr.app.core.stt

class RamCalculator {

    fun isSafeForDevice(model: ModelInfo, freeRamMb: Long): Boolean {
        // Un margine di sicurezza del 15% sulla RAM libera per prevenire OOM
        val safetyBufferMb = (freeRamMb * 0.85).toLong()
        return model.ramRequiredMb <= safetyBufferMb
    }

    fun selectSmartDefaultModel(availableModels: List<ModelInfo>, freeRamMb: Long): ModelInfo? {
        val safeModels = availableModels.filter { isSafeForDevice(it, freeRamMb) }
        
        // Se c'è un modello cap ideale ed è sicuro, usa quello per non sprecare RAM
        val idealCap = safeModels.firstOrNull { it.isIdealCap }
        if (idealCap != null) {
            return idealCap
        }

        // Altrimenti scegli il modello con le prestazioni più elevate tra quelli sicuri
        return safeModels.maxByOrNull { it.ramRequiredMb }
    }
}
