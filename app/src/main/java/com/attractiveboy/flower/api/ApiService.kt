package com.attractiveboy.flower.api


import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface ApiService {
    @POST("appLogin")
    fun login(@Body jsonBody: Map<String, String>): Call<ResponseBody>

    @GET("wms/receiptOrder/list")
    fun getReceiptOrderList(@QueryMap params: Map<String, String>): Call<ResponseBody>
}