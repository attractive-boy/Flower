package com.attractiveboy.flower.index

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.attractiveboy.flower.R
import com.attractiveboy.flower.inbound.InboundActivity
import com.attractiveboy.flower.login.LoginActivity
import com.attractiveboy.flower.outbound.OutboundActivity

class IndexActivity : AppCompatActivity() {

    private lateinit var btnInbound: Button
    private lateinit var btnOutbound: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnInbound = findViewById(R.id.btnInbound)
        btnOutbound = findViewById(R.id.btnOutbound)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupClickListeners() {
        btnInbound.setOnClickListener {
            startActivity(Intent(this, InboundActivity::class.java))
        }

        btnOutbound.setOnClickListener {
            startActivity(Intent(this, OutboundActivity::class.java))
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定") { _, _ ->
                    // 清除登录状态
                    getSharedPreferences("flower_user", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                    
                    // 跳转到登录页面
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
