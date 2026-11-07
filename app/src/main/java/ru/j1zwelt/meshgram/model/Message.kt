package ru.j1zwelt.meshgram.model

import java.text.SimpleDateFormat
import java.util.Locale

data class Message(val text: String, val isFromMe: Boolean, val time: Long = System.currentTimeMillis()) {
    companion object {
        fun Long.toFormattedTime(): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(this)
        }
    }
}
