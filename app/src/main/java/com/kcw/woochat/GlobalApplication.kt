package com.kcw.woochat

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData

class GlobalApplication: Application() {
    private val chatUserID: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun setChatID(id: String) {
        chatUserID.value = id
        Log.d("SET ID", id)
    }

    fun getChatID(): String? {
        Log.d("GET ID", chatUserID.value.toString())
        return chatUserID.value
    }
}