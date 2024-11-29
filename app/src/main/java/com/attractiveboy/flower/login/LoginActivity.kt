package com.attractiveboy.flower.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.lifecycleScope
import com.attractiveboy.flower.api.ApiService
import com.attractiveboy.flower.databinding.ActivityLoginBinding
import com.attractiveboy.flower.index.IndexActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val apiService: ApiService = RetrofitClient.instance.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化RetrofitClient
        RetrofitClient.init(this)

        // 检查是否已经登录
        val token = getSharedPreferences("flower_user", MODE_PRIVATE).getString("auth_token", null)
        if (!token.isNullOrEmpty()) {
            navigateToHome()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val loginButton = binding.login
        val loading = binding.loading

        // 恢复保存的用户名和密码
        val prefs = getSharedPreferences("flower_user", MODE_PRIVATE)
        val savedUsername = prefs.getString("username", "")
        val savedPassword = prefs.getString("password", "")

        (binding.username as AppCompatEditText).setText(savedUsername)
        (binding.password as AppCompatEditText).setText(savedPassword)

        loginButton.setOnClickListener {
            val usernameInput = (binding.username as AppCompatEditText).text?.toString()?.trim() ?: ""
            val passwordInput = (binding.password as AppCompatEditText).text?.toString()?.trim() ?: ""

            if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loading.visibility = View.VISIBLE
            loginButton.isEnabled = false

            performLogin(usernameInput, passwordInput, loading, loginButton)
        }
    }

    private fun performLogin(username: String, password: String, loading: View, loginButton: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = login(username, password)
                result.onSuccess { jsonObject ->
                    // 从返回的JsonObject中获取token
                    val token = jsonObject.getAsJsonObject("data").get("token").asString
                    if (!token.isNullOrEmpty()) {
                        // 保存token、用户名和密码到SharedPreferences
                        getSharedPreferences("flower_user", MODE_PRIVATE)
                            .edit()
                            .putString("auth_token", token)
                            .putString("username", username)
                            .putString("password", password)
                            .apply()
                        
                        withContext(Dispatchers.Main) {
                            handleLoginResult(true, loading, loginButton)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            handleLoginResult(false, loading, loginButton)
                        }
                    }
                }.onFailure { exception ->
                    withContext(Dispatchers.Main) {
                        handleLoginError(exception as Exception, loading, loginButton)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleLoginError(e, loading, loginButton)
                }
            }
        }
    }

    private fun handleLoginResult(isSuccess: Boolean, loading: View, loginButton: View) {
        loading.visibility = View.GONE
        loginButton.isEnabled = true
        if (isSuccess) {
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
            navigateToHome()
        } else {
            Toast.makeText(this, "登录失败，请检查用户名或密码", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLoginError(e: Exception, loading: View, loginButton: View) {
        loading.visibility = View.GONE
        loginButton.isEnabled = true
        Toast.makeText(this, "发生错误：${e.message}", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, IndexActivity::class.java)
        startActivity(intent)
        finish()
    }

    suspend fun login(username: String, password: String): Result<JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = mapOf(
                    "username" to username,
                    "password" to password
                )

                val response: Response<ResponseBody> = apiService.login(jsonBody).execute()
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()?.string()
                    // 解析响应体，假设你使用Gson
                    val gson = Gson()
                    val loginResult = gson.fromJson(responseBody, JsonObject::class.java)
                    Result.success(loginResult)
                } else {
                    Result.failure(HttpException(response))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
