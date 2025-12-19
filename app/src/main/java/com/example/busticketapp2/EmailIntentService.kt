package com.example.busticketapp2

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class EmailIntentService(private val context: Context) {
    companion object {
        private const val TAG = "EmailIntentService"

        fun generateVerificationCode(): String {
            return Random().nextInt(999999).toString().padStart(6, '0')
        }
    }

    fun sendVerificationCodeViaIntent(email: String, code: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "Код подтверждения регистрации - Автобусные Билеты")

                val emailText = """
                    Код подтверждения: $code
                    
                    Введите этот код в приложении "Автобусные Билеты".
                    Код действителен 15 минут.
                """.trimIndent()

                putExtra(Intent.EXTRA_TEXT, emailText)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(intent, "Выберите почтовое приложение"))
                true
            } else {
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка: ${e.message}")
            false
        }
    }
}