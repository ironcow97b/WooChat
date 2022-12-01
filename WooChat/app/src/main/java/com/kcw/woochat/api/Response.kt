package com.kcw.woochat.api

import com.google.firebase.firestore.auth.User
import com.google.gson.annotations.SerializedName

data class JoinResponse(
    @SerializedName("code") val code: Int
)

data class LoginResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("name") val name: String
)

data class ChangeResponse(
    @SerializedName("code") val code: Int
)

data class GoodByeResponse(
    @SerializedName("code") val code: Int
)

data class UserListResponse(
    @SerializedName("data") val data: List<UserObject>
)

data class UserObject(
    @SerializedName("userID") val userID: String,
    @SerializedName("userName") val userName: String
)

data class SaveResponse(
    @SerializedName("code") val code: Int
)