package com.ttl.robotcontrol

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ttl.robotcontrol.BuildConfig.DEBUG
import com.ttl.robotcontrol.databinding.RobotstatsgridBinding


class SerialText1(var serialText:String){

}


class RobotStatsItem(
    var parameterIcon: Int,
    var parameter: String,
    var parameterValue: String,
    var parameterProgress: MutableLiveData<Int>
) : SeekBar.OnSeekBarChangeListener{
    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, p2: Boolean) {
        parameterProgress.value = progress
        Log.d("Progress","Updating progress bar")
    }
    override fun onStartTrackingTouch(p0: SeekBar?) {
    }
    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

}

class RobotStatsGridAdapter(var arrayList: ArrayList<RobotStatsItem>) :
    RecyclerView.Adapter<RobotStatsGridAdapter.ViewHolder>() {

    class ViewHolder(val binding: RobotstatsgridBinding) :
        RecyclerView.ViewHolder(binding.root) {        ///to do something with views here later
        fun bind(robotStat: RobotStatsItem) {
            binding.robotStat = robotStat
            binding.executePendingBindings()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RobotstatsgridBinding.inflate(layoutInflater)
        return ViewHolder(binding) //ViewHolderRobot(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.robotstatsgrid, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(arrayList[position])
//        holder.bind(arrayList[position])
        //        holder.binding.robotStats = arrayList[position]
//        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }
}