package com.attractiveboy.flower.inbound

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.attractiveboy.flower.databinding.ItemBarcodeBinding

// 条形码数据类


class BarcodeAdapter : ListAdapter<BarcodeItem, BarcodeAdapter.BarcodeViewHolder>(BarcodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val binding = ItemBarcodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BarcodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class BarcodeViewHolder(
        private val binding: ItemBarcodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BarcodeItem) {
            binding.apply {
                tvBarcode.text = "${item.barcode}"
            }
        }
    }
}

private class BarcodeDiffCallback : DiffUtil.ItemCallback<BarcodeItem>() {
    override fun areItemsTheSame(oldItem: BarcodeItem, newItem: BarcodeItem): Boolean {
        return oldItem.barcode == newItem.barcode
    }

    override fun areContentsTheSame(oldItem: BarcodeItem, newItem: BarcodeItem): Boolean {
        return oldItem == newItem
    }
} 