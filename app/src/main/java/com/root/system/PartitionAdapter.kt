package com.root.system

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.root.system.R

class PartitionAdapter(
    private val partitions: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PartitionAdapter.PartitionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartitionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_partition, parent, false)
        return PartitionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartitionViewHolder, position: Int) {
        val partition = partitions[position]
        holder.bind(partition)
        holder.itemView.setOnClickListener {
            onItemClick(partition)
        }
    }

    override fun getItemCount(): Int {
        return partitions.size
    }

    class PartitionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val partitionName: TextView = itemView.findViewById(R.id.partition_name)

        fun bind(partition: String) {
            partitionName.text = partition
        }
    }
}
