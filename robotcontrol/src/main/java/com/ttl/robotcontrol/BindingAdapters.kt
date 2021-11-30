package com.ttl.robotcontrol

import android.widget.ImageView
import android.widget.SeekBar
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.adapters.SeekBarBindingAdapter
import androidx.recyclerview.widget.RecyclerView

@BindingAdapter(value = ["setImageID"])
fun ImageView.bindImage(imageID : Int){
    this.setImageResource(imageID)
}

@BindingAdapter(value = ["setRecyclerViewAdapter"])
fun RecyclerView.bindRecyclerViewAdapter(adapter: RecyclerView.Adapter<*>){
    this.run {
        this.setHasFixedSize(true)
        this.adapter = adapter
    }
}

@BindingAdapter(value = ["SeekBarChangeListener"])
fun SeekBar.bindSeekBarChangeListener(listener: SeekBar.OnSeekBarChangeListener){
    this.setOnSeekBarChangeListener(listener)
}

//@BindingAdapter(value = ["ProgressChanged"])
//fun onProgressChanged(seekBar:SeekBar, progress:Int, fromUser:Boolean){
//
//}