package com.kcw.woochat.model

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.kcw.woochat.api.RetrofitService
import com.kcw.woochat.dataclass.UserData
import io.grpc.Context.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.io.File

class MainModel(application: Application) {
    private val myApp = application

    suspend fun uploadImage(imageFile: File): Boolean {
        val data = CoroutineScope(Dispatchers.IO).async {
            val userInfo = myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
            val userID = userInfo.getString("userID", "").toString()

            val storage = Firebase.storage("gs://woochat-5e57c.appspot.com")
            val storageRef = storage.reference

            val db = Firebase.firestore

            if (userID.isNotBlank()) {
                val dbReq = db.collection("User").document(userID).get().await()

                if (dbReq.data != null) {
                    val photoName = dbReq.data!!["photoName"] as String

                    Log.d("PREV_PHOTO", photoName)

                    if (photoName.isNotBlank()) {
                        storageRef.child(photoName).delete()
                    }
                }
            }

            var result = false

            val imageRef = storageRef.child(imageFile.name)
            imageRef.putFile(Uri.fromFile(imageFile)).addOnCompleteListener {
                if (it.isSuccessful) {
                    result = true

                    if (imageFile.path.contains("com.kcw.woochat")) { //내부 저장소의 파일만 삭제하도록
                        val deleted = imageFile.delete()

                        if (deleted) {
                            Log.d("temp Image deleted", imageFile.name)
                        }
                    }
                }
            }.await()

            if (result) {
                db.collection("User").document(userID).update("photoName", imageFile.name)
                    .addOnCompleteListener {
                        if (!it.isSuccessful) {
                            result = false
                        }
                    }.await()
            }

            result
        }

        return data.await()
    }

    suspend fun changePassword(password: String): Int {
        val data = CoroutineScope(Dispatchers.IO).async {
            val userInfo = myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
            val userID = userInfo.getString("userID", "").toString()

            val params = HashMap<String, String>()
            params["userID"] = userID
            params["newPwd"] = password

            var code = -1

            val response = RetrofitService.apis.changePassword(params).awaitResponse()

            if (response.isSuccessful) {
                if (response.code() == 200 && response.body() != null) {
                    code = response.body()!!.code
                }
            }

            code
        }

        return data.await()
    }

    suspend fun goodBye(): Int {
        val data = CoroutineScope(Dispatchers.IO).async {
            val userInfo = myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
            val userInfoEdit = userInfo.edit()
            val userID = userInfo.getString("userID", "").toString()

            val storage = Firebase.storage("gs://woochat-5e57c.appspot.com")
            val storageRef = storage.reference

            val db = Firebase.firestore

            if (userID.isNotBlank()) {
                val dbReq = db.collection("User").document(userID).get().await()

                if (dbReq.data != null) {
                    val photoName = dbReq.data!!["photoName"] as String

                    Log.d("PREV_PHOTO", photoName)

                    if (photoName.isNotBlank()) {
                        storageRef.child(photoName).delete()
                    }

                    db.collection("User").document(userID).delete()
                }
            }

            val params = HashMap<String, String>()
            params["userID"] = userID

            var code = -1

            val response = RetrofitService.apis.goodBye(params).awaitResponse()

            if (response.isSuccessful) {
                if (response.code() == 200 && response.body() != null) {
                    code = response.body()!!.code

                    userInfoEdit.clear()
                    userInfoEdit.apply()
                }
            }

            code
        }

        return data.await()
    }

    suspend fun getUserList(): Bundle {
        val data = CoroutineScope(Dispatchers.IO).async {
            val userInfo = myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
            val userID = userInfo.getString("userID", "").toString()

            val params = HashMap<String, String>()
            params["userID"] = userID

            val bundle = Bundle()

            val userList = ArrayList<UserData>()

            val response = RetrofitService.apis.getUserList(params).awaitResponse()

            if (response.isSuccessful) {
                if (response.code() == 200 && response.body() != null) {
                    for (i in 0 until response.body()!!.data.size) {
                        userList.add(
                            UserData(
                                response.body()!!.data[i].userID,
                                response.body()!!.data[i].userName
                            )
                        )
                    }
                }
            }

            bundle.putSerializable("userList", userList)

            bundle
        }

        return data.await()
    }

    suspend fun saveFireToken(token: String): Int {
        val data = CoroutineScope(Dispatchers.IO).async {
            val userInfo = myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
            val userID = userInfo.getString("userID", "").toString()

            val params = HashMap<String, String>()
            params["userID"] = userID
            params["token"] = token

            var code = -1

            val response = RetrofitService.apis.saveFireToken(params).awaitResponse()

            if (response.isSuccessful) {
                if (response.code() == 200 && response.body() != null) {
                    code = response.body()!!.code
                }
            }

            code
        }

        return data.await()
    }

    fun sendPush(message: String, userID: String) {
        val userInfo = myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
        val myUserID = userInfo.getString("userID", "").toString()
        val myName = userInfo.getString("userName", "").toString()

        val params = HashMap<String, String>()
        params["userID"] = userID
        params["myUserID"] = myUserID
        params["myName"] = myName
        params["message"] = message

        RetrofitService.apis.sendPush(params).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.code() == 200) {
                    Log.d("SEND_PUSH_REQ", "OK")
                } else {
                    Log.d("SEND_PUSH_REQ", "FAILED")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("SEND_PUSH_REQ", "FAILED _ ${t.localizedMessage}")
            }
        })
    }
}