package com.attractiveboy.flower.inbound

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.attractiveboy.flower.api.ApiService
import com.attractiveboy.flower.databinding.ActivityInboundDetailBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response

class InboundDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInboundDetailBinding
    private var inboundOrder: InboundOrder? = null
    private val apiService: ApiService = RetrofitClient.instance.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboundDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从Intent中获取订单ID
        val orderId = intent.getLongExtra("order_id", -1L)
        
        if (orderId == -1L) {
            Toast.makeText(this, "获取订单ID失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化RetrofitClient
        RetrofitClient.init(this)

        // 加载订单详情
        loadOrderDetail(orderId)

        // 设置返回按钮
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadOrderDetail(orderId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = getReceiptOrderDetail(orderId)
                result.onSuccess { jsonObject ->
                    withContext(Dispatchers.Main) {
                        val gson = Gson()
                        inboundOrder = gson.fromJson(jsonObject, InboundOrder::class.java)
                        setupUI()
                        generateBarcode()
                    }
                }.onFailure { exception ->
                    withContext(Dispatchers.Main) {
                        handleError(exception as Exception)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleError(e)
                }
            }
        }
    }

    private fun setupUI() {
        inboundOrder?.let { order ->
            binding.apply {
                // 设置订单基本信息
                tvOrderNo.text = "订单编号：${order.orderNo}"
                tvCreateTime.text = "创建时间：${order.createTime}"
                tvStatus.text = "订单状态：${
                    when(order.orderStatus) {
                        "0" -> "待审核"
                        "1" -> "已审核"
                        "2" -> "已完成"
                        else -> "未知状态"
                    }
                }"
                tvSupplierName.text = "供应商：${order.supplierName}"
                tvTotalAmount.text = "总金额：¥${String.format("%.2f", order.totalAmount)}"
                tvRemark.text = "备注：${order.remark ?: "无"}"
            }
        }
    }

    private fun generateBarcode() {
        try {
            inboundOrder?.let { order ->
                val multiFormatWriter = MultiFormatWriter()
                val bitMatrix: BitMatrix = multiFormatWriter.encode(
                    order.orderNo,
                    BarcodeFormat.CODE_128,
                    800,
                    200
                )
                val barcodeEncoder = BarcodeEncoder()
                val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
                binding.ivBarcode.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "生成条码失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleError(e: Exception) {
        Log.e("InboundDetailActivity", "发生错误", e)
        Toast.makeText(this, "发生错误：${e.message}", Toast.LENGTH_SHORT).show()
    }

    private suspend fun getReceiptOrderDetail(id: Long): Result<JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<ResponseBody> = apiService.getReceiptOrderDetail(id).execute()
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()?.string()
                    val gson = Gson()
                    val result = gson.fromJson(responseBody, JsonObject::class.java)
                    Result.success(result)
                } else {
                    Result.failure(HttpException(response))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}