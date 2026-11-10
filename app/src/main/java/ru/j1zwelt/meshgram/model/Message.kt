package ru.j1zwelt.meshgram.model

import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale

data class Message(
    val text: String = "",
    val isFromMe: Boolean = false,
    val fromUser: String? = null,
    val toUser: String? = null,
    val time: Long = System.currentTimeMillis()
) {
    fun toGson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun Long.toFormattedTime(): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(this)
        }

        fun String?.toMessage(): Message {
            return if (this != null)
                Gson().fromJson(this, Message::class.java)
            else Message()
        }
    }
}
