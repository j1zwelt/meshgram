package ru.j1zwelt.meshgram.model

import com.google.gson.Gson

data class User(val name: String?, val userName: String) {
    fun toGson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun String?.toUser(): User {
            return if (this != null)
                Gson().fromJson(this, User::class.java)
            else User("", "")
        }
    }
}
