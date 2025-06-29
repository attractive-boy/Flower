package com.attractiveboy.flower.outbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.R

data class WarehousedItem(
    val itemName: String,
    val skuName: String,
    val itemSerialNumber: String?,
    val itemStatus: String?,
    val itemPrice: Double,
    val itemQuantity: Double,
    val itemSkuId: String,

)

class WarehousedBarcodeAdapter : RecyclerView.Adapter<WarehousedBarcodeAdapter.ViewHolder>() {
    private val items = mutableListOf<WarehousedItem>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemInfo: TextView = view.findViewById(R.id.tvItemInfo)
        val tvBarcode: TextView = view.findViewById(R.id.tvBarcode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_warehoused_barcode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvItemInfo.text = "${item.itemName} 数量：${item.itemQuantity} 总价：${item.itemPrice} "
        // holder.tvBarcode.text = "条码: ${item.itemSerialNumber}"
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<WarehousedItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
