package com.kcw.woochat.view

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.kcw.woochat.R
import kotlinx.coroutines.tasks.await

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        if ((requireActivity() as MainActivity).myPhoto != null) {
            val bitmap = (requireActivity() as MainActivity).myPhoto
            view.findViewById<ImageView>(R.id.iv_photo).setImageBitmap(bitmap)
        } else if ((requireActivity() as MainActivity).photoUri != null) {
            val uri = (requireActivity() as MainActivity).photoUri
            view.findViewById<ImageView>(R.id.iv_photo).setImageURI(uri)
        } else { //Firebase에 저장된 사진은 Glide를 통해 직접 ImageView에 표시한다.
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_progress)
            dialog.findViewById<TextView>(R.id.tv_progress).text = "정보를 불러오는 중..."
            dialog.show()

            val userInfo =
                requireActivity().getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
            val userID = userInfo.getString("userID", "").toString()

            var photoName = ""

            val db = Firebase.firestore
            db.collection("User").document(userID).get()
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
                                        Glide.with(requireContext())
                                            .load(url)
                                            .into(view.findViewById<ImageView>(R.id.iv_photo))

                                        val bitmapDrawable: BitmapDrawable? =
                                            view.findViewById<ImageView>(R.id.iv_photo).drawable as BitmapDrawable?
                                        val bitmap: Bitmap? = bitmapDrawable?.bitmap

                                        (requireActivity() as MainActivity).myPhoto = bitmap
                                        dialog.dismiss()
                                    }
                                    .addOnFailureListener {
                                        dialog.dismiss()
                                    }
                            } else {
                                dialog.dismiss()
                            }
                        }
                    }
                }
        }

        view.findViewById<EditText>(R.id.et_name)
            .setText((requireActivity() as MainActivity).userName)
        view.findViewById<EditText>(R.id.et_id).setText((requireActivity() as MainActivity).userID)

        view.findViewById<Button>(R.id.btn_change_password).setOnClickListener {
            val dialog = (requireActivity() as MainActivity).changeDialog
            dialog.setContentView(R.layout.dialog_change_pwd)
            dialog.findViewById<Button>(R.id.btn_change_password).setOnClickListener {
                val password =
                    dialog.findViewById<EditText>(R.id.et_password).text.toString().trim()
                val pwdConfirm =
                    dialog.findViewById<EditText>(R.id.et_pwd_confirm).text.toString().trim()

                if (password != pwdConfirm) {
                    Toast.makeText(requireContext(), "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }

                (requireActivity() as MainActivity).viewModel.changePassword(password)
            }
            dialog.show()
        }

        view.findViewById<ImageView>(R.id.iv_photo).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("사진 등록")
                .setMessage("사진을 촬영하거나 앨범에서 선택해 주세요")
                .setPositiveButton("사진 촬영") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    (requireActivity() as MainActivity).checkCamPermission()
                }
                .setNegativeButton("사진 선택") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    (requireActivity() as MainActivity).checkAlbumPermission()
                }
                .create()
                .show()
        }

        view.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말로 로그아웃 하겠습니까?")
                .setPositiveButton("확인", DialogInterface.OnClickListener { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    (requireActivity() as MainActivity).logout()
                })
                .setNegativeButton("취소", DialogInterface.OnClickListener { dialogInterface, _ ->
                    dialogInterface.dismiss()
                })
                .create()
                .show()
        }

        view.findViewById<TextView>(R.id.tv_goodbye).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("회원탈퇴")
                .setMessage("정말로 WooChat을 떠나시겠습니까?")
                .setPositiveButton("확인", DialogInterface.OnClickListener { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    (requireActivity() as MainActivity).viewModel.goodBye()
                })
                .setNegativeButton("취소", DialogInterface.OnClickListener { dialogInterface, _ ->
                    dialogInterface.dismiss()
                })
                .create()
                .show()
        }

        return view
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}