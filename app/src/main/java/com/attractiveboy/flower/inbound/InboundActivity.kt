package com.attractiveboy.flower.inbound

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

class InboundActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInboundBinding
    private val apiService: ApiService = RetrofitClient.instance.create(ApiService::class.java)
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true
    private var currentKeyword: String? = null
    private lateinit var inboundAdapter: InboundAdapter
    private val inboundList = mutableListOf<InboundOrder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化RetrofitClient
        RetrofitClient.init(this)

        setupUI()
        loadInboundData()
    }

    private fun setupUI() {
        // 初始化适配器
        inboundAdapter = InboundAdapter(inboundList)
        
        // 设置RecyclerView
        binding.inboundList.apply {
            layoutManager = LinearLayoutManager(this@InboundActivity)
            adapter = inboundAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadMoreData()
                        }
                    }
                }
            })
        }
        
        // 设置下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            resetAndRefresh()
        }

        // 设置搜索
        binding.searchButton.setOnClickListener {
            val keyword = binding.searchInput.text.toString().trim()
            currentKeyword = if (keyword.isNotEmpty()) keyword else null
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
        if (currentPage == 1) {
            binding.swipeRefresh.isRefreshing = true
        }

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
                        val newOrders = mutableListOf<InboundOrder>()
                        rows.forEach { element ->
                            val order = gson.fromJson(element, InboundOrder::class.java)
                            newOrders.add(order)
                        }
                        
                        // 如果是第一页,清空原有数据
                        if (currentPage == 1) {
                            inboundList.clear()
                        }
                        inboundList.addAll(newOrders)
                        
                        // 更新适配器
                        inboundAdapter.notifyDataSetChanged()
                        
                        // 更新空视图状态
                        binding.emptyView.visibility = if (inboundList.isEmpty()) View.VISIBLE else View.GONE
                        binding.inboundList.visibility = if (inboundList.isEmpty()) View.GONE else View.VISIBLE
                        
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
        pageSize: Int = 10,
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