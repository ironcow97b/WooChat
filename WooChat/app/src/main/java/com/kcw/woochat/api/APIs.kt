package com.kcw.woochat.api

import retrofit2.Call
import retrofit2.http.*

interface APIs {
    @FormUrlEncoded
    @POST("/join_user.php")
    fun joinUser(@FieldMap params: HashMap<String, String>): Call<JoinResponse>

    @FormUrlEncoded
    @POST("/login_user.php")
    fun loginUser(@FieldMap params: HashMap<String, String>): Call<LoginResponse>

    @FormUrlEncoded
    @POST("/change_pwd.php")
    fun changePassword(@FieldMap params: HashMap<String, String>): Call<ChangeResponse>

    @FormUrlEncoded
    @POST("/good_bye.php")
    fun goodBye(@FieldMap params: HashMap<String, String>): Call<GoodByeResponse>

    @FormUrlEncoded
    @POST("/get_user_list.php")
    fun getUserList(@FieldMap params: HashMap<String, String>): Call<UserListResponse>

    @FormUrlEncoded
    @POST("/save_fire_token.php")
    fun saveFireToken(@FieldMap params: HashMap<String, String>): Call<SaveResponse>

    @FormUrlEncoded
    @POST("/send_push.php")
    fun sendPush(@FieldMap params: HashMap<String, String>): Call<Void>
}