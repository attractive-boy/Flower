import android.annotation.SuppressLint
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.attractiveboy.flower.login.LoginActivity
import com.google.gson.Gson
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer

object RetrofitClient {
    private const val BASE_URL = "http://8.155.19.17:12000/prod-api/"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun bodyToString(request: RequestBody?): String {
        return try {
            val buffer = Buffer()
            request?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            "无法读取请求体"
        }
    }

    @SuppressLint("StaticFieldLeak")
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = context?.getSharedPreferences("flower_user", Context.MODE_PRIVATE)
            ?.getString("auth_token", null)

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        // 打印请求信息
        val requestBody = bodyToString(newRequest.body())
        val requestLog = StringBuilder().apply {
            appendLine("\n╔════════════════════════ Request ════════════════════════")
            appendLine("║ URL: ${newRequest.url()}")
            appendLine("║ Method: ${newRequest.method()}")
            appendLine("║ Headers:")
            newRequest.headers().names().forEach { name ->
                appendLine("║    $name: ${newRequest.header(name)}")
            }
            if (requestBody.isNotEmpty()) {
                appendLine("║ Body: $requestBody")
            }
            appendLine("╚═══════════════════════════════════════════════════════════")
        }
        // 修改为 INFO 级别,确保日志可以输出
        Log.i("RetrofitClient", requestLog.toString())

        val response = chain.proceed(newRequest)
        
        // 打印响应信息
        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        val responseLog = StringBuilder().apply {
            appendLine("\n╔════════════════════════ Response ═══════════════════════")
            appendLine("║ URL: ${response.request().url()}")
            appendLine("║ Status Code: ${response.code()}")
            appendLine("║ Headers:")
            response.headers().names().forEach { name ->
                appendLine("║    $name: ${response.header(name)}")
            }
            appendLine("║ Body: $responseBody")
            appendLine("╚═══════════════════════════════════════════════════════════")
        }
        // 修改为 INFO 级别,确保日志可以输出
        Log.i("RetrofitClient", responseLog.toString())

        val gson = Gson()
        val jsonObject = gson.fromJson(responseBody, Map::class.java)
        
        if (jsonObject["code"] == 401.0) {
            // 使用Handler在主线程中执行跳转
            mainHandler.post {
                context?.let {
                    // 清除token
                    it.getSharedPreferences("flower_user", Context.MODE_PRIVATE)
                        .edit()
                        .remove("auth_token")
                        .apply()
                        
                    val intent = Intent(it, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.startActivity(intent)
                }
            }
        }
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}