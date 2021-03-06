package com.ttl.robotcontrol

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ttl.robotcontrol.databinding.SerialTextBinding
class SerialText(val serialText:String, val color: Int = Color.WHITE)

class SerialTextRecyclerAdapter(private val serialTextArray: ArrayList<SerialText>) :
    RecyclerView.Adapter<SerialTextRecyclerAdapter.ViewHolder>() {

    class ViewHolder(val binding: SerialTextBinding) :
        RecyclerView.ViewHolder(binding.root) {        ///to do something with views here later
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SerialTextBinding.inflate(layoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(serialTextArray[position])
        holder.binding.serialText.text = serialTextArray[position].serialText
        holder.binding.serialText.setTextColor(serialTextArray[position].color)
    }

    override fun getItemCount(): Int {
        return serialTextArray.size
    }

}