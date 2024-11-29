import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context

object RetrofitClient {
    private const val BASE_URL = "http://8.155.19.17/prod-api/"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

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

        chain.proceed(newRequest)
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