package ru.j1zwelt.meshgram.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson

data class Chat(
    val user: User,
    val messages: SnapshotStateList<Message>
) {
    companion object {
        fun String?.toChatsList(): SnapshotStateList<Chat> {
            val list = mutableStateListOf<Chat>()
            if (this.isNullOrBlank()) return list

            val array = Gson().fromJson(this, Array<Chat>::class.java)
            if (array != null) {
                list.addAll(array)
            }

            return list
        }
    }
}