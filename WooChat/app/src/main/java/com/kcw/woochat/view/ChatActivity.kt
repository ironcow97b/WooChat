package com.kcw.woochat.view

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.kcw.woochat.GlobalApplication
import com.kcw.woochat.R
import com.kcw.woochat.databinding.ActivityChatBinding
import com.kcw.woochat.dataclass.ChatData
import com.kcw.woochat.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[MainViewModel::class.java]
    }
    private val binding: ActivityChatBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_chat)
    }

    private val chatList = ArrayList<ChatData>()
    private val chatListAdapter = ChatListAdapter(chatList)

    lateinit var userID: String
    lateinit var userName: String

    lateinit var myUserID: String
    lateinit var myUserName: String

    var myPhoto: Bitmap? = null

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = viewModel

        binding.rvChat.adapter = chatListAdapter

        val userInfo = getSharedPreferences("userInfo", MODE_PRIVATE)
        myUserID = userInfo.getString("userID", "").toString()
        myUserName = userInfo.getString("userName", "").toString()

        userID = intent.getStringExtra("userID") ?: ""
        userName = intent.getStringExtra("userName") ?: ""

        binding.tvName.text = userName

        (application as GlobalApplication).setChatID(userID)

        val controlManager: InputMethodManager =
            getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager

        val db = Firebase.firestore

        chatList.clear()

        db.collection("Chat").document(myUserID).collection(userID)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("Chat", "Listen failed: $error")
                    return@addSnapshotListener
                }

                if (value!!.metadata.isFromCache) {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("userID", userID)
                    intent.putExtra("userName", userName)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    startActivity(intent)

                    finish()

                    return@addSnapshotListener
                }

                for (doc in value.documentChanges) {
                    if (doc.type == DocumentChange.Type.ADDED) {
                        chatList.add(
                            ChatData(
                                doc.document["sender"].toString(),
                                doc.document["receiver"].toString(),
                                doc.document["message"].toString(),
                                doc.document["time"].toString(),
                                doc.document["isChecked"] as Boolean
                            )
                        )
                    }

                    chatListAdapter.notifyDataSetChanged()

                    binding.rvChat.scrollToPosition(chatList.size - 1)
                }
            }

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.etMessage.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.rvChat.scrollToPosition(chatList.size - 1)
            }, 100)
        }

        binding.etMessage.onFocusChangeListener = OnFocusChangeListener { _, _ ->
            Handler(Looper.getMainLooper()).postDelayed({
                binding.rvChat.scrollToPosition(chatList.size - 1)
            }, 100)
        }

        binding.tvSend.setOnClickListener {
            sendMsg()
        }
    }

    override fun onResume() {
        super.onResume()

        notificationManager.deleteNotificationChannel(userID)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                userID,
                "WooChat",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    @SuppressLint("SimpleDateFormat")
    private fun sendMsg() {
        val msg = binding.etMessage.text.toString()

        val timeMillis = System.currentTimeMillis()

        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+09:00")

        val time = dateFormat.format(timeMillis)

        val data = mapOf(
            "sender" to myUserID,
            "receiver" to userID,
            "time" to time,
            "message" to msg,
            "isChecked" to false
        )

        val db = Firebase.firestore

        db.collection("Chat").document(myUserID).collection(userID).document(timeMillis.toString())
            .set(data)
            .addOnSuccessListener {
                db.collection("Chat").document(userID).collection(myUserID)
                    .document(timeMillis.toString())
                    .set(data)
                    .addOnSuccessListener {
                        val data2 = mapOf(
                            userID to userName
                        )

                        db.collection("Chat").document(myUserID).get() //필드가 하나라도 존재하는 지 체크
                            .addOnSuccessListener {
                                val data3 = mapOf(
                                    myUserID to myUserName
                                )

                                if (!it.data.isNullOrEmpty()) { //내 필드에 해당 필드가 이미 존재할 때
                                    db.collection("Chat").document(myUserID).update(data2)
                                        .addOnSuccessListener {
                                            db.collection("Chat").document(userID)
                                                .get() //상대 필드에 해당 필드가 있는 지 확인
                                                .addOnSuccessListener { it2 ->
                                                    if (!it2.data.isNullOrEmpty()) { //필드가 하나라도 존재하는 지 체크
                                                        db.collection("Chat").document(userID)
                                                            .update(data3)
                                                            .addOnSuccessListener {
                                                                binding.etMessage.setText("")
                                                                viewModel.sendPush(msg, userID)
                                                            }
                                                            .addOnFailureListener {
                                                                clearFailMsg(timeMillis.toString())
                                                            }
                                                    } else { //상대 필드에 해당 필드가 존재하지 않을 때
                                                        db.collection("Chat").document(userID)
                                                            .set(data3)
                                                            .addOnSuccessListener {
                                                                binding.etMessage.setText("")
                                                                viewModel.sendPush(msg, userID)
                                                            }
                                                            .addOnFailureListener {
                                                                clearFailMsg(timeMillis.toString())
                                                            }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    clearFailMsg(timeMillis.toString())
                                                }
                                        }
                                        .addOnFailureListener {
                                            clearFailMsg(timeMillis.toString())
                                        }
                                } else { //내 필드에 해당 필드가 존재하지 않을 때
                                    db.collection("Chat").document(myUserID).set(data2)
                                        .addOnSuccessListener {
                                            db.collection("Chat").document(userID)
                                                .get() //상대 필드에 해당 필드가 있는 지 확인
                                                .addOnSuccessListener { it2 ->
                                                    if (!it2.data.isNullOrEmpty()) { //상대 필드에 해당 필드가 이미 존재할 때
                                                        db.collection("Chat").document(userID)
                                                            .update(data3)
                                                            .addOnSuccessListener {
                                                                binding.etMessage.setText("")
                                                                viewModel.sendPush(msg, userID)
                                                            }
                                                            .addOnFailureListener {
                                                                clearFailMsg(timeMillis.toString())
                                                            }
                                                    } else { //상대 필드에 해당 필드가 존재하지 않을 때
                                                        db.collection("Chat").document(userID)
                                                            .set(data3)
                                                            .addOnSuccessListener {
                                                                binding.etMessage.setText("")
                                                                viewModel.sendPush(msg, userID)
                                                            }
                                                            .addOnFailureListener {
                                                                clearFailMsg(timeMillis.toString())
                                                            }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    clearFailMsg(timeMillis.toString())
                                                }
                                        }
                                        .addOnFailureListener {
                                            clearFailMsg(timeMillis.toString())
                                        }
                                }
                            }
                            .addOnFailureListener {
                                clearFailMsg(timeMillis.toString())
                            }
                    }
                    .addOnFailureListener {
                        clearFailMsg(timeMillis.toString())
                    }
            }
            .addOnFailureListener {
                clearFailMsg(timeMillis.toString())
            }
    }

    private fun clearFailMsg(msgID: String) {
        Toast.makeText(this, "메세지 전송을 실패했습니다!", Toast.LENGTH_SHORT).show()

        val db = Firebase.firestore

        db.collection("Chat").document(myUserID).collection(userID).document(msgID).delete()
            .addOnSuccessListener {
                db.collection("Chat").document(myUserID).get().addOnSuccessListener {
                    if (it.data != null) {
                        if (it.data!!.isNotEmpty() && it.data!!.containsKey(userID)) {
                            val data = mapOf(
                                userID to FieldValue.delete()
                            )

                            db.collection("Chat").document(myUserID).update(data)
                        }
                    }
                }
            }

        db.collection("Chat").document(userID).collection(myUserID).document(msgID).delete()
            .addOnSuccessListener {
                db.collection("Chat").document(userID).get().addOnSuccessListener {
                    if (it.data != null) {
                        if (it.data!!.isNotEmpty() && it.data!!.containsKey(myUserID)) {
                            val data = mapOf(
                                myUserID to FieldValue.delete()
                            )

                            db.collection("Chat").document(userID).update(data)
                        }
                    }
                }
            }
    }

    inner class ChatListAdapter(
        private val items: ArrayList<ChatData>
    ) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            private val view: View = v

            fun bind(item: ChatData, myPos: Int) {
                val ivPhoto = view.findViewById<ImageView>(R.id.iv_photo)
                val prevPos = myPos - 1
                val tvDay = view.findViewById<TextView>(R.id.tv_day)
                val layoutDay = view.findViewById<LinearLayout>(R.id.layout_day)

                if (item.sender == myUserID) {
                    val tvMyUserName = view.findViewById<TextView>(R.id.tv_myUserName)

                    view.findViewById<LinearLayout>(R.id.layout_msg).visibility = GONE
                    view.findViewById<LinearLayout>(R.id.layout_my_msg).visibility = VISIBLE

                    /*
                    if (!item.isChecked) {
                        view.findViewById<TextView>(R.id.tv_check).visibility = VISIBLE
                    } else {
                        view.findViewById<TextView>(R.id.tv_check).visibility = GONE
                    }
                     */

                    tvMyUserName.text = myUserName
                    view.findViewById<TextView>(R.id.tv_myMessage).text = item.message

                    if (prevPos >= 0) {
                        if (items[prevPos].sender == item.sender) {
                            tvMyUserName.visibility = GONE
                        }

                        val myTime = item.time.split(" ")
                        val prevTime = items[prevPos].time.split(" ")
                        if (prevTime.size == 2 && myTime.size == 2) {
                            if (prevTime[0] == myTime[0]) {
                                view.findViewById<TextView>(R.id.tv_myTime).text = myTime[1]

                                layoutDay.visibility = GONE
                            } else {
                                tvDay.text = myTime[0]
                                view.findViewById<TextView>(R.id.tv_myTime).text = myTime[1]

                                layoutDay.visibility = VISIBLE
                            }
                        }
                    } else {
                        val myTime = item.time.split(" ")
                        if (myTime.size == 2) {
                            tvDay.text = myTime[0]
                            view.findViewById<TextView>(R.id.tv_myTime).text = myTime[1]

                            layoutDay.visibility = VISIBLE
                        }
                    }
                } else {
                    val tvUserName = view.findViewById<TextView>(R.id.tv_userName)

                    view.findViewById<LinearLayout>(R.id.layout_msg).visibility = VISIBLE
                    view.findViewById<LinearLayout>(R.id.layout_my_msg).visibility = GONE

                    tvUserName.text = userName
                    view.findViewById<TextView>(R.id.tv_message).text = item.message
                    view.findViewById<TextView>(R.id.tv_time).text = item.time

                    if (prevPos >= 0) {
                        if (items[prevPos].sender == item.sender) {
                            view.findViewById<LinearLayout>(R.id.layout_user_info).visibility = GONE
                        } else {
                            view.findViewById<LinearLayout>(R.id.layout_user_info).visibility = VISIBLE
                        }

                        val myTime = item.time.split(" ")
                        val prevTime = items[prevPos].time.split(" ")
                        if (prevTime.size == 2 && myTime.size == 2) {
                            if (prevTime[0] == myTime[0]) {
                                view.findViewById<TextView>(R.id.tv_time).text = myTime[1]

                                layoutDay.visibility = GONE
                            } else {
                                tvDay.text = myTime[0]
                                view.findViewById<TextView>(R.id.tv_time).text = myTime[1]

                                layoutDay.visibility = VISIBLE
                            }
                        }
                    } else {
                        val myTime = item.time.split(" ")
                        if (myTime.size == 2) {
                            tvDay.text = myTime[0]
                            view.findViewById<TextView>(R.id.tv_time).text = myTime[1]

                            layoutDay.visibility = VISIBLE
                        }
                    }
                }

                if (myPhoto == null) {
                    val db = Firebase.firestore

                    db.collection("User").document(userID).get()
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                if (it.result.data != null) {
                                    val photoName = it.result.data!!["photoName"] as String

                                    if (photoName.isNotBlank()) {
                                        val storage =
                                            Firebase.storage("gs://woochat-5e57c.appspot.com")
                                        val storageRef = storage.reference

                                        val imageRef = storageRef.child(photoName)
                                        imageRef.downloadUrl
                                            .addOnSuccessListener { url ->
                                                try {
                                                    Glide.with(this@ChatActivity)
                                                        .load(url)
                                                        .into(view.findViewById(R.id.iv_photo))

                                                    val bitmapDrawable: BitmapDrawable? =
                                                        ivPhoto.drawable as BitmapDrawable?
                                                    val bitmap: Bitmap? = bitmapDrawable?.bitmap

                                                    myPhoto = bitmap
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                    }
                                }
                            }
                        }
                } else {
                    view.findViewById<ImageView>(R.id.iv_photo).setImageBitmap(myPhoto)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat, parent, false)

            return ViewHolder(inflatedView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.apply {
                bind(item, position)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }
}
