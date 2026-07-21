package it.tldl.app.core.stt

import android.app.ActivityManager
import android.content.Context

object RamCalculator {
    fun getAvailableRamMb(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    fun getTotalRamMb(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }

    fun isSafeForDevice(model: ModelInfo, availableRamMb: Long): Boolean {
        return model.ramRequiredMb <= availableRamMb * 0.8
    }

    fun selectSmartDefaultModel(models: List<ModelInfo>, availableRamMb: Long): ModelInfo? {
        val safeModels = models.filter { isSafeForDevice(it, availableRamMb) }
        val idealCap = safeModels.find { it.isIdealCap }
        if (idealCap != null) return idealCap
        return safeModels.maxByOrNull { it.ramRequiredMb }
    }
}
