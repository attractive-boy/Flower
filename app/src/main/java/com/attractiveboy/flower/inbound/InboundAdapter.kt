package com.attractiveboy.flower.inbound

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.R
import com.attractiveboy.flower.databinding.ItemInboundOrderBinding

class InboundAdapter(inboundList: MutableList<InboundOrder>) : RecyclerView.Adapter<InboundAdapter.ViewHolder>() {

    private var orders = mutableListOf<InboundOrder>()
    private var onItemClickListener: ((InboundOrder) -> Unit)? = null
    private var onScanClickListener: ((InboundOrder) -> Unit)? = null

    fun setData(newOrders: List<InboundOrder>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (InboundOrder) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnScanClickListener(listener: (InboundOrder) -> Unit) {
        onScanClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInboundOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size

    inner class ViewHolder(
        private val binding: ItemInboundOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: InboundOrder) {
            binding.apply {
                // 设置订单编号
                tvOrderNo.text = order.orderNo

                // 设置创建时间
                tvCreateTime.text = order.createTime

                // 设置订单状态
                tvStatus.text = order.orderStatus
                tvStatus.setBackgroundResource(R.drawable.status_background)

                // 设置供应商名称
                tvSupplierName.text = order.supplierName

                // 设置总金额
                tvTotalAmount.text = String.format("¥%.2f", order.totalAmount)

                // 设置备注
                tvRemark.text = order.remark ?: "无"

                // 设置扫描按钮点击事件
                btnScan.setOnClickListener {
                    onScanClickListener?.invoke(order)
                }

                // 设置整个item的点击事件
                root.setOnClickListener {
                    onItemClickListener?.invoke(order)
                }
            }
        }
    }
}