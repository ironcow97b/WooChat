package com.kcw.woochat.model

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kcw.woochat.api.RetrofitService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import retrofit2.awaitResponse

class UserModel(application: Application) {
    private val myApp = application

    suspend fun joinUser(name: String, id: String, password: String): Int {
        val data = CoroutineScope(Dispatchers.IO).async {
            val params = HashMap<String, String>()
            params["id"] = id
            params["password"] = password
            params["name"] = name

            var code = -1

            val data = mapOf(
                "id" to id,
                "name" to name,
                "photoName" to ""
            )

            var fsResult = false

            val db = Firebase.firestore
            db.collection("User").document(id).set(data)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d("Firestore", "User 정보 입력을 완료했습니다!")

                        fsResult = true
                    } else {
                        Log.d("Firestore", "User 정보 입력을 실패했습니다!")
                    }
                }.await()

            if (fsResult) {
                val response = RetrofitService.apis.joinUser(params).awaitResponse()

                if (response.isSuccessful) {
                    if (response.code() == 200 && response.body() != null) {
                        code = response.body()!!.code

                        Log.d("JoinCode", "$code")
                    }
                }
            }

            code
        }

        return data.await()
    }

    suspend fun loginUser(id: String, password: String, loginType: Int): Int {
        val data = CoroutineScope(Dispatchers.IO).async {
            val params = HashMap<String, String>()
            params["id"] = id
            params["password"] = password

            val response = RetrofitService.apis.loginUser(params).awaitResponse()

            var code = -1

            if (response.isSuccessful) {
                if (response.code() == 200 && response.body() != null) {
                    code = response.body()!!.code
                    val name = response.body()!!.name

                    val userInfo =
                        myApp.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
                    val userInfoEdit = userInfo.edit()

                    userInfoEdit.putString("userName", name)
                    userInfoEdit.putString("userID", id)
                    userInfoEdit.putString("userPassword", password)
                    userInfoEdit.putInt("loginType", loginType)
                    userInfoEdit.apply()
                }
            }

            code
        }

        return data.await()
    }
}