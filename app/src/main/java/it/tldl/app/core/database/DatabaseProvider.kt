package it.tldl.app.core.database

import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import java.util.Base64

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = buildEncryptedDatabase(context.applicationContext)
            INSTANCE = instance
            instance
        }
    }

    private fun buildEncryptedDatabase(context: Context): AppDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphrase = getOrCreatePassphrase(context)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tldl_encrypted.db"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration(false)
        .build()
    }

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences("tldl_keystore_prefs", Context.MODE_PRIVATE)
        val encBase64 = prefs.getString("db_passphrase_enc_b64", null)
        val ivBase64 = prefs.getString("db_passphrase_iv_b64", null)
        val legacyBase64 = prefs.getString("db_passphrase_b64", null)

        return try {
            if (encBase64 != null && ivBase64 != null) {
                val encBytes = Base64.getDecoder().decode(encBase64)
                val ivBytes = Base64.getDecoder().decode(ivBase64)
                decryptWithKeyStore(encBytes, ivBytes)
            } else if (legacyBase64 != null) {
                // Migrate legacy unencrypted key to KeyStore encryption
                val plainBytes = Base64.getDecoder().decode(legacyBase64)
                val (encrypted, iv) = encryptWithKeyStore(plainBytes)
                prefs.edit()
                    .putString("db_passphrase_enc_b64", Base64.getEncoder().encodeToString(encrypted))
                    .putString("db_passphrase_iv_b64", Base64.getEncoder().encodeToString(iv))
                    .remove("db_passphrase_b64")
                    .apply()
                plainBytes
            } else {
                // Generate fresh 32-byte key and encrypt with KeyStore
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                val (encrypted, iv) = encryptWithKeyStore(randomBytes)
                prefs.edit()
                    .putString("db_passphrase_enc_b64", Base64.getEncoder().encodeToString(encrypted))
                    .putString("db_passphrase_iv_b64", Base64.getEncoder().encodeToString(iv))
                    .apply()
                randomBytes
            }
        } catch (e: Exception) {
            // Fallback for JVM unit test environments where KeyStore is unbacked
            if (legacyBase64 != null) {
                Base64.getDecoder().decode(legacyBase64)
            } else {
                val fallbackKey = ByteArray(32)
                SecureRandom().nextBytes(fallbackKey)
                fallbackKey
            }
        }
    }

    private fun encryptWithKeyStore(plainBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val secretKey = getOrCreateKeyStoreKey()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(plainBytes)
        return Pair(encrypted, cipher.iv)
    }

    private fun decryptWithKeyStore(encryptedBytes: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = getOrCreateKeyStoreKey()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encryptedBytes)
    }

    private fun getOrCreateKeyStoreKey(): java.security.Key {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "tldl_db_key_alias"
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val builder = android.security.keystore.KeyGenParameterSpec.Builder(
                alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        return keyStore.getKey(alias, null)
    }
}
