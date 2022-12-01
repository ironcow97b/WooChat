package com.kcw.woochat.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.storage.StorageReference
import com.kcw.woochat.repository.MainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val myApp = application
    private val myRepo = MainRepository(myApp)

    val uploadResult: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val uploadSelResult: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val changeResult: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val goodByeResult: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val userListResult: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }

    val saveResult: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun uploadImage(imageFile: File) {
        CoroutineScope(Dispatchers.Main).launch {
            myRepo.uploadImage(imageFile)

            val result = myRepo.getUploadResult()
            uploadResult.value = result
        }
    }

    fun uploadSelImage(imageFile: File) {
        CoroutineScope(Dispatchers.Main).launch {
            myRepo.uploadImage(imageFile)

            val result = myRepo.getUploadResult()
            uploadSelResult.value = result
        }
    }

    fun changePassword(password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            myRepo.changePassword(password)

            val result = myRepo.getChangeResult()
            changeResult.value = result
        }
    }

    fun goodBye() {
        CoroutineScope(Dispatchers.Main).launch {
            myRepo.goodBye()

            val result = myRepo.getGoodByeResult()
            goodByeResult.value = result
        }
    }

    fun getUserList() {
        CoroutineScope(Dispatchers.Main).launch {
            myRepo.getUserList()

            val result = myRepo.getUserResult()
            userListResult.value = result
        }
    }

    fun saveFireToken(token: String) {
        CoroutineScope(Dispatchers.Main).launch {
            myRepo.saveFireToken(token)

            val result = myRepo.getSaveResult()
            saveResult.value = result
        }
    }

    fun sendPush(message: String, userID: String) {
        myRepo.sendPush(message, userID)
    }
}