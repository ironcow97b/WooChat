package com.kcw.woochat.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kcw.woochat.R
import com.kcw.woochat.databinding.ActivityJoinBinding
import com.kcw.woochat.viewmodel.UserViewModel

class JoinActivity : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[UserViewModel::class.java]
    }
    private val binding: ActivityJoinBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_join)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = viewModel

        setObserver()

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun setObserver() {
        val joinObserver = Observer<Int?> { joinCode ->
            when (joinCode) {
                2 -> {
                    Toast.makeText(this, "이미 등록된 아이디입니다", Toast.LENGTH_SHORT).show()
                }
                0, -1 -> {
                    Toast.makeText(this, "오류가 발생했습니다!", Toast.LENGTH_SHORT).show()
                }
                -2 -> {
                    Toast.makeText(this, "빈칸을 모두 채워주세요", Toast.LENGTH_SHORT).show()
                }
                -3 -> {
                    Toast.makeText(this, "비밀번호 확인이 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                }
                1 ->  { //기본 코드가 -1이므로 else는 성공 -> joinCode == 1
                    Toast.makeText(this, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()

                    finish()
                }
            }
        }
        viewModel.joinCode.observe(this, joinObserver)
    }
}