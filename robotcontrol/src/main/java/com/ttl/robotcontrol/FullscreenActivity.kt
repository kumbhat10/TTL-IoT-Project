package com.ttl.robotcontrol

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.ttl.robotcontrol.databinding.ActivityFullscreenBinding


class FullscreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var viewModel : FullscreenViewModel
    private var precision = 3
    private var maxDegree = 180
//    viewModel =     ViewModelProvider(this).get(FullscreenViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this)[FullscreenViewModel::class.java]
        binding.viewModel1 = viewModel // bind view model in XML layout to our viewModel
    }

    private fun seekBarListener(){
        binding.seekBar.max = maxDegree/precision
        binding.seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress1: Int, fromUser: Boolean) {
                val progress = progress1*precision
                binding.textValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    @SuppressLint("SetTextI18n")
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        viewModel.onGenericMotionEvent(event)
        return super.onGenericMotionEvent(event)
    }

    fun toastCenter(message: String) {
        Snackbar.make(binding.mainLayout, message, Snackbar.LENGTH_LONG).setAction("Dismiss") {}
            .setActionTextColor(getColor(R.color.borderblue)).show()
    }

//    @SuppressLint("SetTextI18n")
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
////        toastCenter(keyCode.toString())
////        binding.toast.text = keyCode.toString() + " Key Down"
//        return true //super.onKeyDown(keyCode, event)
//    }

}