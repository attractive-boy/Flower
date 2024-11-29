package com.attractiveboy.flower.inbound

data class InboundOrder(
    val id: Long = 0,
    val orderNo: String = "", // 订单编号
    val createTime: String = "", // 创建时间
    val orderStatus: String = "", // 订单状态
    val supplierName: String = "", // 供应商名称
    val totalAmount: Double = 0.0, // 总金额
    val remark: String? = null // 备注
)
