package com.attractiveboy.flower.outbound


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.api.ApiService
import com.attractiveboy.flower.databinding.ActivityOutboundBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

class OutboundActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOutboundBinding
    private val apiService: ApiService = RetrofitClient.instance.create(ApiService::class.java)
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true
    private var currentKeyword: String? = null
    private lateinit var outboundAdapter: OutboundAdapter<OutboundOrder>
    private val outboundList = mutableListOf<OutboundOrder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutboundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化RetrofitClient
        RetrofitClient.init(this)

        // 设置返回按钮点击事件
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupUI()
        setupSwipeRefresh()
        setupSearch()
        loadOutboundData()
    }

    private fun setupSearch() {
        binding.searchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                currentKeyword = binding.searchInput.text.toString().trim()
                resetAndRefresh()
                return@setOnEditorActionListener true
            }
            false
        }

        // 自动获取焦点
        binding.searchInput.requestFocus()

        binding.searchInput.setSelection(binding.searchInput.text.length)

        // 隐藏软键盘
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)

        // 设置窗口的软键盘模式
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        //失去焦点时自动获取
        binding.searchInput.setOnFocusChangeListener { v, hasFocus ->
            binding.searchInput.requestFocus()
        }
    }

    private fun setupUI() {
        // 初始化适配器
        outboundAdapter = OutboundAdapter(outboundList)
        
        // 设置RecyclerView
        binding.outboundList.apply {
            // 确保设置固定大小
            setHasFixedSize(true)
            
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(this@OutboundActivity).also {
                Log.d("OutboundAdapter", "LayoutManager set")
            }
            
            // 设置适配器
            adapter = outboundAdapter.also {
                Log.d("OutboundAdapter", "Adapter set")
            }
            
            // 添加分割线
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            
            // 确保可见性
            visibility = View.VISIBLE

            // 添加滚动监听器实现触底加载
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount

                    // 当滑动到最后一个item，且不在加载中，且还有更多数据时，触发加载
                    if (lastVisibleItemPosition == totalItemCount - 1 && !isLoading && hasMoreData) {
                        loadMoreData()
                    }
                }
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            resetAndRefresh()
        }
    }

    private fun resetAndRefresh() {
        currentPage = 1
        hasMoreData = true
        outboundList.clear()
        outboundAdapter.notifyDataSetChanged()
        loadOutboundData()
    }

    private fun loadMoreData() {
        if (!isLoading && hasMoreData) {
            currentPage++
            loadOutboundData()
        }
    }

    private fun loadOutboundData() {
        if (isLoading) return
        
        isLoading = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = getShipmentOrderList(
                    orderNo = currentKeyword,
                    pageNum = currentPage
                )
                result.onSuccess { jsonObject ->
                    withContext(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = false
                        val rows = jsonObject.getAsJsonArray("rows")
                        val total = jsonObject.get("total").asInt

                        // 解析数据并添加到列表
                        val gson = Gson()
                        
                        // 清空列表（如果是第一页）
                        if (currentPage == 1) {
                            outboundList.clear()
                        }
                        
                        // 添加新数据
                        rows.forEach { element ->
                            val order = gson.fromJson(element, OutboundOrder::class.java)
                            outboundList.add(order)
                            Log.d("OutboundAdapter", "Added order: $order")
                        }
                        
                        // 通知适配器数据变化
                        outboundAdapter.notifyDataSetChanged()
                        
                        // 更新UI状态
                        binding.outboundList.visibility = if (outboundList.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyView.visibility = if (outboundList.isEmpty()) View.VISIBLE else View.GONE
                        
                        Log.d("OutboundAdapter", "Data loaded, list size: ${outboundList.size}")
                        
                        // 判断是否还有更多数据
                        hasMoreData = outboundList.size < total
                        
                        if (currentPage == 1) {
                            Toast.makeText(this@OutboundActivity, "加载成功,共${total}条数据", Toast.LENGTH_SHORT).show()
                        }
                        
                        isLoading = false
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

    private fun handleError(e: Exception) {
        Log.e("OutboundActivity", "发生错误", e)
        binding.swipeRefresh.isRefreshing = false
        isLoading = false
    }

    suspend fun getShipmentOrderList(
        orderNo: String? = null,
        orderStatus: String? = null,
        pageSize: Int = 2,
        pageNum: Int = 1,
        orderByColumn: String? = null,
        isAsc: String? = null
    ): Result<JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                val params = mutableMapOf<String, String>()
                orderNo?.let { params["orderNo"] = it }
                orderStatus?.let { params["orderStatus"] = it }
                params["pageSize"] = pageSize.toString()
                params["pageNum"] = pageNum.toString()
                params["orderStatus"] = "0"
                orderByColumn?.let { params["orderByColumn"] = it }
                isAsc?.let { params["isAsc"] = it }

                val response: Response<ResponseBody> = apiService.getShipmentOrderList(params).execute()
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