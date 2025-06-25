package com.attractiveboy.flower.api


import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface ApiService {
    @POST("appLogin")
    fun login(@Body jsonBody: Map<String, String>): Call<ResponseBody>

    @GET("wms/receiptOrder/mobile-list")
    fun getReceiptOrderList(@QueryMap params: Map<String, String>): Call<ResponseBody>

    @GET("wms/receiptOrder/get-receipt-order-info")
    fun getReceiptOrderDetail(@QueryMap params: Map<String, String>): Call<ResponseBody>

    @GET("wms/receiptOrder/close-lock")
    fun closeReceiptOrderLock(@QueryMap params: Map<String, String>): Call<ResponseBody>

    @POST("wms/receiptOrder/warehousing")
    fun submitWarehousing( @Body jsonBody: Map<String, Any>): Call<ResponseBody>

    @POST("wms/receiptOrder")
    fun createReceiptOrder (@Body jsonBody: Map<String, Any>): Call<ResponseBody>

    data class SubmitReceiptOrderRequest(
        val bizOrderNo: String,
        val serialNumberList: List<String>
    )

    @POST("wms/receiptOrder/submit-recript-order")
    fun submitReceiptOrder(@Body request: SubmitReceiptOrderRequest): Call<ResponseBody>

    // 出库单列表
    @GET("wms/shipmentOrder/list")
    fun getShipmentOrderList(@QueryMap params: Map<String, String>): Call<ResponseBody>

    // 出库单详情
    @GET("wms/shipmentOrder/get-shipment-order-info")
    fun getShipmentOrderDetail(@QueryMap params: Map<String, String>): Call<ResponseBody>

    // 关闭出库单锁定
    @GET("wms/shipmentOrder/close-lock")
    fun closeShipmentOrderLock(@QueryMap params: Map<String, String>): Call<ResponseBody>

    // 提交出库
    data class SubmitShipmentOrderDetailReqVo(
        val skuId: Long,
        val quantity: Double?,
        val amount: Double,
        val remark: String? = null
    )

    data class SubmitShipmentOrderRequest(
        val bizOrderNo: String,
        val details: List<SubmitShipmentOrderDetailReqVo>
    )

    @POST("wms/shipmentOrder/submit-shipment-order")
    fun submitShipmentOrder(@Body request: SubmitShipmentOrderRequest): Call<ResponseBody>

    // 创建出库单
    @POST("wms/shipmentOrder")
    fun createShipmentOrder(@Body jsonBody: Map<String, Any>): Call<ResponseBody>

    //获取出库单条码的详细信息
    @GET("wms/itemSkuDetail/get-item-sku-detail/{code}")
    fun getItemSkuDetail(@Path("code") code: String): Call<ResponseBody>
    
}