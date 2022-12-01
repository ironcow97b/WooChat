package com.kcw.woochat.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.kcw.woochat.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val myApp = application
    private val myRepo = UserRepository(myApp)

    val id: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val password: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val pwConfirm: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val name: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val loginType: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>(0)
    }

    val joinCode: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    val loginCode: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun joinUser() {
        if (name.value.isNullOrBlank() || id.value.isNullOrBlank() || password.value.isNullOrBlank() || pwConfirm.value.isNullOrBlank()) { //가입 정보가 하나라도 없으면
            joinCode.value = -2
            return
        } else if (password.value != pwConfirm.value) { //비밀번호와 비밀번호 확인이 일치하지 않으면
            joinCode.value = -3
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            myRepo.joinUser(name.value!!.trim(), id.value!!.trim(), password.value!!.trim())

            val result = myRepo.getJoinCode()
            joinCode.value = result
        }
    }

    fun loginUser() {
        if (id.value.isNullOrBlank() || password.value.isNullOrBlank()) {
            loginCode.value = -2
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            myRepo.loginUser(id.value!!.trim(), password.value!!.trim(), 2)

            val result = myRepo.getLoginCode()
            loginCode.value = result
        }
    }
}