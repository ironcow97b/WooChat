package com.kcw.woochat.repository

import android.app.Application
import com.kcw.woochat.model.UserModel

class UserRepository(application: Application) {
    private val myApp = application
    private val myModel = UserModel(myApp)

    private var joinCode = -1
    private var loginCode = -1

    suspend fun joinUser(name: String, id: String, password: String) {
        setJoinCode(myModel.joinUser(name, id, password))
    }

    private fun setJoinCode(code: Int) {
        joinCode = code
    }

    fun getJoinCode(): Int {
        return joinCode
    }

    suspend fun loginUser(id: String, password: String, loginType: Int) {
        setLoginCode(myModel.loginUser(id, password, loginType))
    }

    private fun setLoginCode(code: Int) {
        loginCode = code
    }

    fun getLoginCode(): Int {
        return loginCode
    }
}