package com.attractiveboy.flower.outbound

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.databinding.ItemOutboundOrderBinding
import com.attractiveboy.flower.outbound.OutboundDetailActivity
import com.attractiveboy.flower.outbound.OutboundOrder

class OutboundAdapter<T>(private val outboundList: MutableList<OutboundOrder>) :
    RecyclerView.Adapter<OutboundAdapter<T>.ViewHolder>() {

    // 添加数据更新方法
    fun setData(newData: List<OutboundOrder>) {
        outboundList.clear()
        outboundList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutboundAdapter<T>.ViewHolder {
        val binding = ItemOutboundOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = outboundList[position]
        holder.bind(order)
    }

    override fun getItemCount(): Int = outboundList.size

    inner class ViewHolder(private val binding: ItemOutboundOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OutboundOrder) {
            binding.apply {
                // 设置订单编号
                tvOrderNo.text = "订单编号：${order.orderNo}"

                // 设置创建时间
                tvCreateTime.text = "创建时间：${order.createTime}"




                // 设置点击事件,跳转到详情页面
                root.setOnClickListener {
                    val intent = Intent(root.context, OutboundDetailActivity::class.java)
                    intent.putExtra("outbound_order", order.id.toString())
                    root.context.startActivity(intent)
                }
            }
        }
    }
}