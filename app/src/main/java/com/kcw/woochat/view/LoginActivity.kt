package com.kcw.woochat.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kcw.woochat.R
import com.kcw.woochat.databinding.ActivityLoginBinding
import com.kcw.woochat.viewmodel.UserViewModel

class LoginActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[UserViewModel::class.java]
    }
    private val binding: ActivityLoginBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_login)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = viewModel

        setObserver()

        val userInfo = getSharedPreferences("userInfo", MODE_PRIVATE)
        val loginType = userInfo.getInt("loginType", 0)

        if (loginType == 1) {
            val userID = userInfo.getString("userID", "").toString()
            viewModel.id.value = userID
            binding.cbSaveId.isChecked = true
        }

        binding.tvJoin.setOnClickListener {
            val intent = Intent(this, JoinActivity::class.java)
            startActivity(intent)
        }

        binding.cbAutoLogin.setOnClickListener {
            if (binding.cbAutoLogin.isChecked) {
                viewModel.loginType.value = 2
            } else {
                if (binding.cbSaveId.isChecked) {
                    viewModel.loginType.value = 1
                } else {
                    viewModel.loginType.value = 0
                }
            }
        }

        binding.cbSaveId.setOnClickListener {
            if (!binding.cbAutoLogin.isChecked) {
                if (binding.cbSaveId.isChecked) {
                    viewModel.loginType.value = 1
                } else {
                    viewModel.loginType.value = 0
                }
            }
        }
    }

    private fun setObserver() {
        val loginObserver = Observer<Int?> { loginCode ->
            when (loginCode) {
                0 -> {
                    Toast.makeText(this, "유저 정보를 확인할 수 없습니다", Toast.LENGTH_SHORT).show()
                }
                -1 -> {
                    Toast.makeText(this, "오류가 발생했습니다!", Toast.LENGTH_SHORT).show()
                }
                -2 -> {
                    Toast.makeText(this, "아이디와 비밀번호를 정확히 입력해 주세요", Toast.LENGTH_SHORT).show()
                }
                1 -> { //기본 코드가 -1이므로 else는 성공 -> loginCode == 1
                    val userInfo = getSharedPreferences("userInfo", MODE_PRIVATE)
                    val userName = userInfo.getString("userName", "")

                    Toast.makeText(this, "${userName}님, 환영합니다!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
        viewModel.loginCode.observe(this, loginObserver)
    }
}