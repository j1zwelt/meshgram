package ru.j1zwelt.meshgram.model

data class Chat(
    val user: User,
    val messages: androidx.compose.runtime.snapshots.SnapshotStateList<Message>
)