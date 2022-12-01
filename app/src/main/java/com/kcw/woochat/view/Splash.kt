package com.kcw.woochat.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.kcw.woochat.R
import com.kcw.woochat.databinding.ActivitySplashBinding
import com.kcw.woochat.viewmodel.UserViewModel

class Splash : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[UserViewModel::class.java]
    }
    private val binding: ActivitySplashBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_splash)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = viewModel

        val userInfo = getSharedPreferences("userInfo", MODE_PRIVATE)
        val loginType = userInfo.getInt("loginType", 0)

        val senderID = intent.getStringExtra("senderID")
        val senderName = intent.getStringExtra("senderName")

        if (!senderID.isNullOrBlank() && !senderName.isNullOrBlank()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("senderID", senderID)
            intent.putExtra("senderName", senderName)
            startActivity(intent)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (loginType == 2) {
                val userID = userInfo.getString("userID", "")
                val userPassword = userInfo.getString("userPassword", "")

                if (userID.isNullOrBlank() || userPassword.isNullOrBlank()) {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }, 1000)
    }
}