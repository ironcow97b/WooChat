package com.kcw.woochat.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.kcw.woochat.GlobalApplication
import com.kcw.woochat.R
import com.kcw.woochat.api.FirebaseMessage
import com.kcw.woochat.databinding.ActivityMainBinding
import com.kcw.woochat.dataclass.ChatData
import com.kcw.woochat.dataclass.ChatRoomData
import com.kcw.woochat.dataclass.UserData
import com.kcw.woochat.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.tasks.await
import org.checkerframework.checker.units.qual.Angle
import java.io.File
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[MainViewModel::class.java]
    }
    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val friendFragment by lazy {
        FriendFragment()
    }
    private val chatFragment by lazy {
        ChatFragment()
    }
    private val settingsFragment by lazy {
        SettingsFragment()
    }

    lateinit var userName: String
    lateinit var userID: String

    var photoUri: Uri? = null
    private var photo: File? = null

    private val photoResultLauncher = //사진 촬영 후
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val storageDir = File(getExternalFilesDir(null), "Pictures")
                val bitmap: Bitmap?

                val option = BitmapFactory.Options()
                val path = storageDir.path + "/" + photo?.name
                bitmap = BitmapFactory.decodeFile(path, option)

                //myPhoto.value = bitmap

                //사진 방향 바로 잡기
                val ei = ExifInterface(path)
                val angle = when (ei.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                resizeBitmap(bitmap, 900f, angle, path)
            } else {
                Toast.makeText(this, "사진 촬영이 취소되었습니다!", Toast.LENGTH_SHORT).show()
                photo = null
                photoUri = null
            }
        }

    private val photoSelResultLauncher = //앨범에서 선택 후
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                if (it.data?.data != null) {
                    val uri = it.data!!.data!!
                    //settingsFragment.view?.findViewById<ImageView>(R.id.iv_photo)?.setImageURI(uri)

                    photoUri = uri
                    getRealPathFromUri(uri)
                }
            }
        }

    var myPhoto: Bitmap? = null

    private val uploadProgress by lazy {
        Dialog(this)
    }

    val changeDialog by lazy {
        Dialog(this)
    }

    private val userList = ArrayList<UserData>()
    val userListAdapter = UserListAdapter(userList)

    private val chatRoomList = ArrayList<ChatRoomData>()
    val chatListAdapter = ChatListAdapter(chatRoomList)

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = viewModel

        setObserver()

        changeFragment(friendFragment)
        initNavigationBar()

        val userInfo = getSharedPreferences("userInfo", MODE_PRIVATE)
        userName = userInfo.getString("userName", "").toString()
        userID = userInfo.getString("userID", "").toString()

        //백그라운드 채팅 푸시 수신 처리
        val senderID = intent.getStringExtra("senderID")
        val senderName = intent.getStringExtra("senderName")

        if (!senderID.isNullOrBlank() && !senderName.isNullOrBlank()) {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("userID", senderID)
                intent.putExtra("userName", senderName)
                startActivity(intent)
            }, 1000)
        }

        //getChatList()

        CoroutineScope(Dispatchers.Main).launch {
            val firebaseMessage = FirebaseMessage()
            val fireToken = firebaseMessage.getFireToken()

            Log.d("GET FIRE TOKEN", fireToken)

            viewModel.saveFireToken(fireToken)
        }

        getHashKey()
    }

    override fun onResume() {
        super.onResume()

        (application as GlobalApplication).setChatID("")
    }

    private fun getHashKey() { //키 해시를 간단하게 구할 수 있는 함수
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        if (packageInfo == null) Log.e("KeyHash", "KeyHash:null")
        for (signature in packageInfo!!.signatures) {
            try {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())

                val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("KeyHash", keyHash)

            } catch (e: NoSuchAlgorithmException) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=$signature", e)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun setObserver() {
        val uploadObserver = Observer<Boolean?> {
            showUploadProgress(false)

            if (it) {
                Toast.makeText(this, "사진 등록을 완료했습니다!", Toast.LENGTH_SHORT).show()

                settingsFragment.view?.findViewById<ImageView>(R.id.iv_photo)
                    ?.setImageBitmap(myPhoto)
            } else {
                Toast.makeText(this, "사진 등록을 실패했습니다!", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.uploadResult.observe(this, uploadObserver)

        val uploadSelObserver = Observer<Boolean?> {
            showUploadProgress(false)

            if (it) {
                Toast.makeText(this, "사진 등록을 완료했습니다!", Toast.LENGTH_SHORT).show()

                settingsFragment.view?.findViewById<ImageView>(R.id.iv_photo)?.setImageURI(photoUri)
            } else {
                Toast.makeText(this, "사진 등록을 실패했습니다!", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.uploadSelResult.observe(this, uploadSelObserver)

        val changeObserver = Observer<Int?> { resultCode ->
            when (resultCode) {
                -1 -> {
                    Toast.makeText(this, "오류가 발생했습니다!", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    Toast.makeText(this, "비밀번호가 변경되었습니다!", Toast.LENGTH_SHORT).show()

                    changeDialog.dismiss()
                }
            }
        }
        viewModel.changeResult.observe(this, changeObserver)

        val goodByeObserver = Observer<Int?> { resultCode ->
            when (resultCode) {
                -1 -> {
                    Toast.makeText(this, "오류가 발생했습니다!", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    Toast.makeText(this, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
        viewModel.goodByeResult.observe(this, goodByeObserver)

        val userListObserver = Observer<Bundle?> {
            userList.clear()

            val tempList = it.getSerializable("userList") as ArrayList<UserData>?
            if (tempList != null) {
                for (i in 0 until tempList.size) {
                    userList.add(tempList[i])
                }
                userListAdapter.notifyDataSetChanged()
            }

            friendFragment.view?.findViewById<TextView>(R.id.tv_count)?.text =
                "친구 (${userList.size})"
        }
        viewModel.userListResult.observe(this, userListObserver)

        val saveObserver = Observer<Int?> { resultCode ->
            when (resultCode) {
                -1 -> {
                    Toast.makeText(this, "토큰 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()

                    finish()
                }
            }
        }
        viewModel.saveResult.observe(this, saveObserver)
    }

    private fun initNavigationBar() {
        binding.bnvMenu.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_friend -> {
                    changeFragment(friendFragment)
                    true
                }
                R.id.menu_chat -> {
                    changeFragment(chatFragment)
                    true
                }
                R.id.menu_settings -> {
                    changeFragment(settingsFragment)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frag_container, fragment)
            .commit()
    }

    fun logout() {
        val userInfo = getSharedPreferences("userInfo", MODE_PRIVATE)
        val userInfoEdit = userInfo.edit()

        userInfoEdit.clear()
        userInfoEdit.apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun checkCamPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { //~ 안드로이드 9
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    val permission = arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestPermissions(permission, 0)
                }
            } else { //안드로이드 10 ~
                takePicture()
            }
        } else {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { //~ 안드로이드 9
                val permission = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestPermissions(permission, 0)
            } else { //안드로이드 10 ~
                val permission = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                requestPermissions(permission, 0)
            }
        }
    }

    fun checkAlbumPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { //~ 안드로이드 9
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                selectPicture()
            } else {
                val permission = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestPermissions(permission, 1)
            }
        } else { //안드로이드 10 ~
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                selectPicture()
            } else {
                val permission = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                requestPermissions(permission, 1)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 0) {
            var allGranted = 1

            for (grant in grantResults) {
                allGranted *= if (grant == PackageManager.PERMISSION_GRANTED) {
                    1
                } else {
                    0
                }
            }

            if (allGranted == 0) {
                AlertDialog.Builder(this)
                    .setTitle("권한 거부됨")
                    .setMessage("권한을 허용하지 않으면 카메라 기능을 이용하실 수 없습니다. [설정]>[권한]에서 권한을 허용해 주세요.")
                    .setPositiveButton("재설정") { dialogInterface, _ ->
                        dialogInterface.dismiss()

                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .create()
                    .show()
            } else {
                takePicture()
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var photoFile: File? = null

        try {
            photoFile = createImageFile()
        } catch (e: IOException) {
            Toast.makeText(this, "오류가 발생했습니다!", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(
                this,
                "com.kcw.woochat.fileprovider",
                photoFile
            )

            Log.d("TAKE_PICTURE", "photoUri : $photoUri")

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intent.putExtra("return-data", true)

            if (intent.resolveActivity(packageManager) != null) {
                photo = photoFile
                photoResultLauncher.launch(intent)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        val format = SimpleDateFormat("yyyyMMddHHmmss")
        val fileName = format.format(Date(System.currentTimeMillis()))

        val storageDir = File(getExternalFilesDir(null), "Pictures")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir.path, "$fileName.jpg")
    }

    private fun resizeBitmap(bitmap: Bitmap, size: Float, angle: Float, path: String) {
        val width = bitmap.width
        val height = bitmap.height

        var newWidth = 0f
        var newHeight = 0f

        if (width > height) {
            newWidth = size
            newHeight = height.toFloat() * (newWidth / width.toFloat())
        } else {
            newHeight = size
            newWidth = width.toFloat() * (newHeight / height.toFloat())
        }

        val scaleWidth = newWidth / width
        val scaleHeight = newHeight / height

        val matrix = Matrix()
        matrix.postRotate(angle);
        matrix.postScale(scaleWidth, scaleHeight)

        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        myPhoto = resizedBitmap

        showUploadProgress(true)

        viewModel.uploadImage(File(path))
    }

    private fun selectPicture() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        photoSelResultLauncher.launch(intent)
    }

    private fun getRealPathFromUri(contentUri: Uri) { //외부 저장소 uri의 실제 uri를 구하는 함수
        val proj = arrayOf(
            MediaStore.Images.Media.DATA
        )

        val cursor: Cursor = managedQuery(contentUri, proj, null, null, null)

        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()

        val path = cursor.getString(columnIndex)
        Log.d("Uri", path)

        showUploadProgress(true)

        viewModel.uploadSelImage(File(path))
    }

    private fun showUploadProgress(show: Boolean) {
        if (show) {
            uploadProgress.setContentView(R.layout.dialog_progress)
            uploadProgress.findViewById<TextView>(R.id.tv_progress).text = "사진을 등록하는 중..."
            uploadProgress.show()
        } else {
            uploadProgress.dismiss()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getChatList() {
        chatRoomList.clear()

        val db = Firebase.firestore

        db.collection("Chat").document(userID).get().addOnSuccessListener {
            if (it.data != null) {
                val keyList = it.data!!.keys.toList()

                for (i in keyList.indices) {
                    val id = keyList[i]
                    val name = it.data!![keyList[i]] as String

                    db.collection("Chat").document(userID).collection(id).get()
                        .addOnSuccessListener { res ->
                            val lastMsg = res.documents[res.documents.size - 1].get("message") as String?
                            var lastTime = res.documents[res.documents.size - 1].get("time") as String?

                            if (lastTime != null) {
                               lastTime = lastTime.split(" ")[0] + "\n" + lastTime.split(" ")[1]
                            }

                            chatRoomList.add(
                                ChatRoomData(
                                    id,
                                    name,
                                    lastMsg ?: "",
                                    lastTime ?: ""
                                )
                            )

                            chatListAdapter.notifyDataSetChanged()
                        }
                }
            }
        }
    }

    inner class UserListAdapter(
        private val items: ArrayList<UserData>
    ) : RecyclerView.Adapter<UserListAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            private val view: View = v

            fun bind(item: UserData) {
                view.findViewById<TextView>(R.id.tv_name).text = item.userName

                var photoName = ""

                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        item.userID,
                        "WooChat",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )

                val db = Firebase.firestore
                db.collection("User").document(item.userID).get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (it.result.data != null) {
                                photoName = it.result.data!!["photoName"] as String

                                if (photoName.isNotBlank()) {
                                    val storage = Firebase.storage("gs://woochat-5e57c.appspot.com")
                                    val storageRef = storage.reference

                                    val imageRef = storageRef.child(photoName)
                                    imageRef.downloadUrl
                                        .addOnSuccessListener { url ->
                                            Glide.with(this@MainActivity)
                                                .load(url)
                                                .into(view.findViewById<ImageView>(R.id.iv_photo))
                                        }
                                }
                            }
                        }
                    }

                view.findViewById<ImageView>(R.id.iv_chat).setOnClickListener {
                    val intent = Intent(this@MainActivity, ChatActivity::class.java)
                    intent.putExtra("userID", item.userID)
                    intent.putExtra("userName", item.userName)
                    startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)

            return ViewHolder(inflatedView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.apply {
                bind(item)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    inner class ChatListAdapter(
        private val items: ArrayList<ChatRoomData>
    ) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            private val view = v

            fun bind(item: ChatRoomData) {
                view.findViewById<TextView>(R.id.tv_userName).text = item.userName
                view.findViewById<TextView>(R.id.tv_lastMsg).text = item.lastMsg
                view.findViewById<TextView>(R.id.tv_lastTime).text = item.lastTime

                val db = Firebase.firestore
                db.collection("User").document(item.userID).get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (it.result.data != null) {
                                val photoName = it.result.data!!["photoName"] as String

                                if (photoName.isNotBlank()) {
                                    val storage = Firebase.storage("gs://woochat-5e57c.appspot.com")
                                    val storageRef = storage.reference

                                    val imageRef = storageRef.child(photoName)
                                    imageRef.downloadUrl
                                        .addOnSuccessListener { url ->
                                            Glide.with(this@MainActivity)
                                                .load(url)
                                                .into(view.findViewById<ImageView>(R.id.iv_photo))
                                        }
                                }
                            }
                        }
                    }

                view.findViewById<LinearLayout>(R.id.layout_item).setOnClickListener {
                    val intent = Intent(this@MainActivity, ChatActivity::class.java)
                    intent.putExtra("userID", item.userID)
                    intent.putExtra("userName", item.userName)
                    startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_room, parent, false)

            return ViewHolder(inflatedView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.apply {
                bind(item)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }
}