package com.ttl.robotcontrol

import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

class FullscreenViewModel : ViewModel() {

    private var servo0 =  Servo(resetPositionAngle = 90)
    private var servo1 =  Servo(resetPositionAngle = 90)
    private var servo2 =  Servo(resetPositionAngle = 90)
    private var servo3 =  Servo(resetPositionAngle = 90)

    var count = 0

    var progress1 = MutableLiveData(0)
    var progress2 = MutableLiveData(0)
    var progress3 = MutableLiveData(0)
    var progress4 = MutableLiveData(0)
    var progress5 = MutableLiveData(0)
    var joystickEnable = MutableLiveData(false)

    fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        val lx = angleFromJoystick(event!!.x)
        val ly = angleFromJoystick(event.y)
        val ry = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_RZ) )
        val rx = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_Z) )
        val tr = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_RTRIGGER) )
        val tl = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_LTRIGGER) )
        val trigger = (tr-tl)+315;

        progress1.value = lx
        progress2.value = ly
        progress3.value = ry
        progress4.value = rx
        progress5.value = trigger

        servo0.angle.value = lx
        servo1.angle.value = ly
        servo2.angle.value = rx
        servo3.angle.value = ry

        count+= 1
        Log.d("ViewModel", "Sending Again  $count")
//        databaseRef.setValue(FirebaseData(lx, ly, rx, ry, trigger))
        return true //super.onGenericMotionEvent(event)
    }

    fun resetESP(){
        progress1.value = 0
        progress2.value = 0
        progress3.value = 0
        progress4.value = 0
        progress5.value = 0
    }

    private fun angleFromJoystick(x: Float, minDegree:Int = 0, maxDegree:Int = 180): Int{
        return (((x*100).roundToInt()) + 315).coerceIn(90, 540)
    }

    private fun gpsDataListener(){

    }

    private fun round1(x: Any, decimals:Int = 1): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(x.toString().toFloat() * multiplier) / multiplier
    }

    private fun radius(x:Double, y:Double):Double {
        return round1(sqrt(x*x + y*y))
    }
}