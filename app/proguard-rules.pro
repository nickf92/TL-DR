# ProGuard rules for Sherpa-ONNX
-keep class com.k2fsa.sherpa.onnx.** { *; }

# ProGuard rules for FFmpegKit
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }

# ProGuard rules for SQLCipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.database.** { *; }

# ProGuard rules for ONNX Runtime
-keep class ai.onnxruntime.** { *; }

