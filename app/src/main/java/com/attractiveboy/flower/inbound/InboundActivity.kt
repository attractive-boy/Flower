package com.attractiveboy.flower.inbound

import InboundAdapter
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
import com.attractiveboy.flower.databinding.ActivityInboundBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class InboundActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInboundBinding
    private val apiService: ApiService = RetrofitClient.instance.create(ApiService::class.java)
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true
    private var currentKeyword: String? = null
    private lateinit var inboundAdapter: InboundAdapter<InboundOrder>
    private val inboundList = mutableListOf<InboundOrder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化RetrofitClient
        RetrofitClient.init(this)

        setupUI()
        setupSwipeRefresh()
        loadInboundData()
    }

    private fun setupUI() {
        // 初始化适配器
        inboundAdapter = InboundAdapter(inboundList)
        
        // 设置RecyclerView
        binding.inboundList.apply {
            // 确保设置固定大小
            setHasFixedSize(true)
            
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(this@InboundActivity).also {
                Log.d("InboundAdapter", "LayoutManager set")
            }
            
            // 设置适配器
            adapter = inboundAdapter.also {
                Log.d("InboundAdapter", "Adapter set")
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
        inboundList.clear()
        inboundAdapter.notifyDataSetChanged()
        loadInboundData()
    }

    private fun loadMoreData() {
        if (!isLoading && hasMoreData) {
            currentPage++
            loadInboundData()
        }
    }

    private fun loadInboundData() {
        if (isLoading) return
        
        isLoading = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = getReceiptOrderList(
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
                            inboundList.clear()
                        }
                        
                        // 添加新数据
                        rows.forEach { element ->
                            val order = gson.fromJson(element, InboundOrder::class.java)
                            inboundList.add(order)
                            Log.d("InboundAdapter", "Added order: $order")
                        }
                        
                        // 通知适配器数据变化
                        inboundAdapter.notifyDataSetChanged()
                        
                        // 更新UI状态
                        binding.inboundList.visibility = if (inboundList.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyView.visibility = if (inboundList.isEmpty()) View.VISIBLE else View.GONE
                        
                        Log.d("InboundAdapter", "Data loaded, list size: ${inboundList.size}")
                        
                        // 判断是否还有更多数据
                        hasMoreData = inboundList.size < total
                        
                        if (currentPage == 1) {
                            Toast.makeText(this@InboundActivity, "加载成功,共${total}条数据", Toast.LENGTH_SHORT).show()
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
        Log.e("InboundActivity", "发生错误", e)
        binding.swipeRefresh.isRefreshing = false
        isLoading = false
        Toast.makeText(this, "发生错误：${e.message}", Toast.LENGTH_SHORT).show()
    }

    suspend fun getReceiptOrderList(
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
                orderByColumn?.let { params["orderByColumn"] = it }
                isAsc?.let { params["isAsc"] = it }

                val response: Response<ResponseBody> = apiService.getReceiptOrderList(params).execute()
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