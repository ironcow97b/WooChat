package com.kcw.woochat.repository

import android.app.Application
import android.net.Uri
import android.os.Bundle
import com.google.firebase.storage.StorageReference
import com.kcw.woochat.model.MainModel
import java.io.File

class MainRepository(application: Application) {
    private val myApp = application
    private val myModel = MainModel(myApp)

    private var uploadResult = false
    private var changeResult = -1
    private var goodByeResult = -1
    private var userListResult: Bundle? = null
    private var saveResult = -1

    //////////////////// Functions ////////////////////

    suspend fun uploadImage(imageFile: File) {
        setUploadResult(myModel.uploadImage(imageFile))
    }

    private fun setUploadResult(result: Boolean) {
        uploadResult = result
    }

    fun getUploadResult(): Boolean {
        return uploadResult
    }

    ///////////////////////////////////////////////////

    suspend fun changePassword(password: String) {
        setChangeResult(myModel.changePassword(password))
    }

    private fun setChangeResult(code: Int) {
        changeResult = code
    }

    fun getChangeResult(): Int {
        return changeResult
    }

    ///////////////////////////////////////////////////

    suspend fun goodBye() {
        setGoodByeResult(myModel.goodBye())
    }

    private fun setGoodByeResult(code: Int) {
        goodByeResult = code
    }

    fun getGoodByeResult(): Int {
        return goodByeResult
    }

    ///////////////////////////////////////////////////

    suspend fun getUserList() {
        setUserResult(myModel.getUserList())
    }

    private fun setUserResult(bundle: Bundle) {
        userListResult = bundle
    }

    fun getUserResult(): Bundle? {
        return userListResult
    }

    ///////////////////////////////////////////////////

    suspend fun saveFireToken(token: String) {
        setSaveResult(myModel.saveFireToken(token))
    }

    private fun setSaveResult(code: Int) {
        saveResult = code
    }

    fun getSaveResult(): Int {
        return saveResult
    }

    ///////////////////////////////////////////////////

    fun sendPush(message: String, userID: String) {
        myModel.sendPush(message, userID)
    }
}