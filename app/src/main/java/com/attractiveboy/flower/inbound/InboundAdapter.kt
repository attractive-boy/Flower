import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.databinding.ItemInboundOrderBinding
import com.attractiveboy.flower.inbound.InboundOrder
import com.attractiveboy.flower.inbound.InboundDetailActivity

class InboundAdapter<T>(private val inboundList: MutableList<InboundOrder>) :
    RecyclerView.Adapter<InboundAdapter<T>.ViewHolder>() {

    // 添加数据更新方法
    fun setData(newData: List<InboundOrder>) {
        inboundList.clear()
        inboundList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboundAdapter<T>.ViewHolder {
        val binding = ItemInboundOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = inboundList[position]
        holder.bind(order)
    }

    override fun getItemCount(): Int = inboundList.size

    inner class ViewHolder(private val binding: ItemInboundOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: InboundOrder) {
            binding.apply {
                // 设置订单编号
                tvOrderNo.text = "订单编号：${order.orderNo}"

                // 设置创建时间
                tvCreateTime.text = "创建时间：${order.createTime}"

                // 设置订单状态
                tvStatus.text = when(order.orderStatus) {
                    "0" -> "待审核"
                    "1" -> "已审核" 
                    "2" -> "已完成"
                    else -> "未知状态"
                }

                // 设置供应商名称
                tvSupplierName.text = "供应商：${order.supplierName}"

                // 设置总金额
                tvTotalAmount.text = "总金额：¥${String.format("%.2f", order.totalAmount)}"

                // 设置备注
                tvRemark.text = "备注：${order.remark ?: "无"}"

                // 设置点击事件,跳转到详情页面
                root.setOnClickListener {
                    val intent = Intent(root.context, InboundDetailActivity::class.java)
                    intent.putExtra("inbound_order", order.id)
                    root.context.startActivity(intent)
                }
            }
        }
    }
}