package com.ttl.robotcontrol

import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

class FullscreenViewModel : ViewModel() {

    var servo0 =  Servo(resetPosition = 90)
    var servo1 =  Servo(resetPosition = 90)
    var servo2 =  Servo(resetPosition = 90)
    var servo3 =  Servo(resetPosition = 90)
    var count = 0
    private val databaseRef =  Firebase.database.getReference("Control/data")

    fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        val lx = angleFromJoystick(event!!.x)
        val ly = angleFromJoystick(event.y)
        val ry = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_RZ) )
        val rx = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_Z) )
        val tr = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_RTRIGGER) )
        val tl = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_LTRIGGER) )
        val trigger = tr-tl;

        servo0.angle.value = lx
        servo1.angle.value = ly
        servo2.angle.value = rx
        servo3.angle.value = ry

        count+= 1
        Log.d("ViewModel", "Sending Again  $count")
        databaseRef.setValue(FirebaseData(lx, ly, ry, rx, trigger))
        return true //super.onGenericMotionEvent(event)
    }

    fun resetESP(){
        databaseRef.setValue(FirebaseData(rt = 1))

    }
    private fun angleFromJoystick(x: Float, minDegree:Int = 0, maxDegree:Int = 180): Int{
        return (((x*100).roundToInt()) + 90).coerceIn(minDegree, maxDegree)

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