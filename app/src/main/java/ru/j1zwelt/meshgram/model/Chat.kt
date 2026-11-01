package ru.j1zwelt.meshgram.model

data class Chat(
    val userName: String,
    val messages: androidx.compose.runtime.snapshots.SnapshotStateList<Message>
)