package com.kcw.woochat.view

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.kcw.woochat.R
import com.kcw.woochat.dataclass.UserData

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_friend, container, false)

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
                                        try {
                                            Glide.with(requireContext())
                                                .load(url)
                                                .into(view.findViewById<ImageView>(R.id.iv_photo))

                                            val bitmapDrawable: BitmapDrawable? =
                                                view.findViewById<ImageView>(R.id.iv_photo).drawable as BitmapDrawable?
                                            val bitmap: Bitmap? = bitmapDrawable?.bitmap

                                            (requireActivity() as MainActivity).myPhoto = bitmap
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }

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

        view.findViewById<TextView>(R.id.tv_name).text = (requireActivity() as MainActivity).userName

        val rvUserList = view.findViewById<RecyclerView>(R.id.rv_userList)
        rvUserList.layoutManager = LinearLayoutManager(requireActivity())
        rvUserList.adapter = (requireActivity() as MainActivity).userListAdapter
        (requireActivity() as MainActivity).viewModel.getUserList()

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FriendFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FriendFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}