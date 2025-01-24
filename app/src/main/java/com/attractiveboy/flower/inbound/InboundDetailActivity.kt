package com.attractiveboy.flower.inbound

import PendingBarcodeAdapter
import WarehousedBarcodeAdapter
import WarehousedItem
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.R
import com.attractiveboy.flower.api.ApiService
import com.attractiveboy.flower.databinding.ActivityInboundDetailBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response

data class OrderDetail(
    val itemName: String,
    val skuName: String, 
    val itemSerialNumber: String,
    val itemStatus: String,
    val skuId: Int,
    val quantity: Int = 1,
    val warehouseId: Int? = null,
    val optType: Int? = null
)

data class OrderResponse(
    val id: Long,
    val orderNo: String,
    val optType: Int,
    val createTime: String,
    val remark: String,
    val warehouseId: Int,
    val details: List<OrderDetail>
)

data class InboundOrder(
    val id: Long,
    val orderNo: String,
    val optType: Int,
    val createTime: String,
    val remark: String?,
    val warehouseId: String
)

data class BarcodeItem(
    val barcode: String,
    val itemName: String?,
    val skuName: String?,
    val status: String,
    val bitmap: Bitmap
)

class InboundDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInboundDetailBinding
    private var inboundOrder: InboundOrder? = null
    private var orderDetails: List<OrderDetail> = emptyList()
    private val apiService: ApiService = RetrofitClient.instance.create(ApiService::class.java)
    private lateinit var scannedBarcodeAdapter: BarcodeAdapter
    private lateinit var warehousedBarcodeAdapter: BarcodeAdapter
    private val scannedBarcodes = mutableListOf<BarcodeItem>()
    private val warehousedBarcodes = mutableListOf<BarcodeItem>()
    private lateinit var pendingAdapter: PendingBarcodeAdapter
    private lateinit var warehousedAdapter: WarehousedBarcodeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboundDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId = intent.getStringExtra("inbound_order")
        
        if (orderId == null) {
            Toast.makeText(this, "获取订单ID失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RetrofitClient.init(this)
        setupRecyclerViews()
        setupBarcodeScan()
        setupButtons()
        
        // Load order details
        lifecycleScope.launch {
            loadOrderDetail(orderId)
        }

        binding.toolbar.setNavigationOnClickListener {
            showExitConfirmDialog()
        }

        // 初始化待入库列表
        val rvScannedList = findViewById<RecyclerView>(R.id.rvScannedList)
        pendingAdapter = PendingBarcodeAdapter()
        rvScannedList.apply {
            layoutManager = LinearLayoutManager(this@InboundDetailActivity)
            adapter = pendingAdapter
        }

        // 初始化已入库列表
        val rvWarehousedList = findViewById<RecyclerView>(R.id.rvWarehousedList)
        warehousedAdapter = WarehousedBarcodeAdapter()
        rvWarehousedList.apply {
            layoutManager = LinearLayoutManager(this@InboundDetailActivity)
            adapter = warehousedAdapter
        }
    }

    private fun setupRecyclerViews() {
        scannedBarcodeAdapter = BarcodeAdapter()
        warehousedBarcodeAdapter = BarcodeAdapter()
        
        binding.rvScannedList.apply {
            layoutManager = LinearLayoutManager(this@InboundDetailActivity)
            adapter = scannedBarcodeAdapter
        }
        
        binding.rvWarehousedList.apply {
            layoutManager = LinearLayoutManager(this@InboundDetailActivity)
            adapter = warehousedBarcodeAdapter
        }
    }

    private fun setupBarcodeScan() {
        binding.etBarcode.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val barcode = v?.text.toString().trim()
                if (barcode.isNotEmpty()) {
                    handleScannedBarcode(barcode)
                    v?.text = ""
                }
                true
            } else {
                true
            }
        }

        // 自动获取焦点
        binding.etBarcode.requestFocus()

        binding.etBarcode.setSelection(binding.etBarcode.text.length)

        // 隐藏软键盘
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etBarcode.windowToken, 0)

        // 设置窗口的软键盘模式
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        //失去焦点时自动获取
        binding.etBarcode.setOnFocusChangeListener { v, hasFocus ->
            binding.etBarcode.requestFocus()
        }
    }

    private fun setupButtons() {
        binding.btnSubmit.setOnClickListener {
            if (scannedBarcodes.isEmpty()) {
                Toast.makeText(this, "请先扫描条码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitWarehousing()
        }
    }

    private fun handleScannedBarcode(barcode: String) {
        // 检查是否已扫描过该条码
        if (scannedBarcodes.any { it.barcode == barcode }) {
            Toast.makeText(this, "该条码已扫描", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(barcode, BarcodeFormat.CODE_128, 800, 200)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            
            val barcodeItem = BarcodeItem(
                barcode = barcode,
                itemName = null,
                skuName = null,
                status = "待入库",
                bitmap = bitmap
            )
            
            // 添加到已扫描列表
            scannedBarcodes.add(barcodeItem)
            scannedBarcodeAdapter.submitList(scannedBarcodes.toList())


            
            // 更新待入库列表
            val currentPendingItems = pendingAdapter.getCurrentList().toMutableList()
            currentPendingItems.add(barcode)
            pendingAdapter.updateData(currentPendingItems)
            
        } catch (e: Exception) {
            Toast.makeText(this, "生成条码失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitWarehousing() {
        inboundOrder?.let { order ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val serialNumberList = scannedBarcodes.map { it.barcode } + warehousedBarcodes.map { it.barcode }
                    val request = ApiService.SubmitReceiptOrderRequest(
                        bizOrderNo = order.orderNo,
                        serialNumberList = serialNumberList
                    )

                    val response = apiService.submitReceiptOrder(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@InboundDetailActivity, "入库成功", Toast.LENGTH_SHORT).show()

                            //刷新界面
                            recreate()

                            
                        } else {
                            Toast.makeText(this@InboundDetailActivity, "入库失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        handleError(e)
                    }
                }
            }
        } ?: Toast.makeText(this, "订单信息不完整", Toast.LENGTH_SHORT).show()
    }

    private suspend fun loadOrderDetail(orderId: String) {
        try {
            val result = getReceiptOrderDetail(orderId)
            result.onSuccess { jsonObject ->
                withContext(Dispatchers.Main) {
                    val gson = Gson()
                    val orderResponse = gson.fromJson(jsonObject.get("data"), OrderResponse::class.java)
                    inboundOrder = InboundOrder(
                        id = orderResponse.id,
                        orderNo = orderResponse.orderNo,
                        optType = orderResponse.optType,
                        createTime = orderResponse.createTime,
                        remark = orderResponse.remark,
                        warehouseId = ""
                    )
                    orderDetails = orderResponse.details
                    
                    // 更新待入库和已入库列表
                    // val pendingItems = orderDetails.filter { it.itemStatus == "0" }
                    //                 .map { it.itemSerialNumber }
                    // pendingAdapter.updateData(pendingItems)
                    
                    val warehousedItems = orderDetails
                                        .map { detail -> 
                                            WarehousedItem(
                                                itemName = detail.itemName,
                                                skuName = detail.skuName,
                                                itemSerialNumber = detail.itemSerialNumber,
                                                itemStatus = detail.itemStatus
                                            )
                                        }
                    warehousedAdapter.updateData(warehousedItems)

                    warehousedBarcodes.addAll(warehousedItems.map { detail ->
                        BarcodeItem(
                            barcode = detail.itemSerialNumber,
                            itemName = detail.itemName,
                            skuName = detail.skuName,
                            status = detail.itemStatus,
                            bitmap = createBarcodeBitmap(detail.itemSerialNumber)
                        )
                    })
                    
                    setupUI()
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

    private fun setupUI() {
        inboundOrder?.let { order ->
            binding.apply {
                tvOrderNo.text = "订单编号：${order.orderNo}"
                tvCreateTime.text = "创建时间：${order.createTime}"
            }
        }
    }

    private fun handleError(e: Exception) {
        Log.e("InboundDetailActivity", "发生错误", e)
    }

    private suspend fun getReceiptOrderDetail(id: String): Result<JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                val params = mapOf("id" to id)
                val response: Response<ResponseBody> = apiService.getReceiptOrderDetail(params).execute()
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()?.string()
                    val gson = Gson()
                    val result = gson.fromJson(responseBody, JsonObject::class.java)
                    if(result.get("code").asInt == 500){
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@InboundDetailActivity, "此订单已被他人锁定，请联系管理员解锁", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    Result.success(result)
                } else {
                    Result.failure(HttpException(response))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("确认退出吗？当前数据不会保存")
            .setPositiveButton("确定") { _, _ ->
                inboundOrder?.let { order ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val params = mapOf("orderNo" to order.orderNo)
                            val response = apiService.closeReceiptOrderLock(params).execute()
                            withContext(Dispatchers.Main) {
                                if (!response.isSuccessful)
                                    Toast.makeText(this@InboundDetailActivity, "释放锁失败", Toast.LENGTH_SHORT).show()
                                }
                                finish()

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@InboundDetailActivity, "释放锁失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                } ?: finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onBackPressed() {
        showExitConfirmDialog()
    }

    private fun createBarcodeBitmap(barcode: String): Bitmap {
        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(barcode, BarcodeFormat.CODE_128, 800, 200)
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.createBitmap(bitMatrix)
    }
}