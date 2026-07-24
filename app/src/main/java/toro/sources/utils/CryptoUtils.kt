package toro.sources.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    // obv this is temporary
    private const val FIXED_KEY = "01234567890123456789012345678901"

    private val secretKey: SecretKey by lazy {
        SecretKeySpec(FIXED_KEY.toByteArray(), "AES")
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and CipherText
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String): String {
        try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            
            val iv = ByteArray(IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            
            val cipherText = ByteArray(combined.size - IV_LENGTH_BYTE)
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            val plainTextBytes = cipher.doFinal(cipherText)
            
            return String(plainTextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Decryption failed"
        }
    }
}