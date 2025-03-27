package com.attractiveboy.flower.outbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.R

class PendingBarcodeAdapter : RecyclerView.Adapter<PendingBarcodeAdapter.ViewHolder>() {
    private val items = mutableListOf<BarcodeItem>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBarcode: TextView = view.findViewById(R.id.tvBarcode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_barcode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val barcode = items[position]
        holder.tvBarcode.text = "${barcode.itemName} 数量：${barcode.quantity} 单价：${barcode.price}"
    }

    override fun getItemCount() = items.size

    fun getCurrentList(): List<BarcodeItem> = items.toList()

    fun updateData(newItems: List<BarcodeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
