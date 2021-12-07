package com.ttl.robotcontrol

import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import kotlin.math.log
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

class FullscreenViewModel : ViewModel() {

    private var servo0 =  Servo(resetPositionAngle = 90)
    private var servo1 =  Servo(resetPositionAngle = 90)
    private var servo2 =  Servo(resetPositionAngle = 90)
    private var servo3 =  Servo(resetPositionAngle = 90)
    private val databaseRef = Firebase.database.getReference("Control")


    var count = 0
    var firebaseData = MutableLiveData(FirebaseData())
    var progress1 = MutableLiveData(0)
    var progress2 = MutableLiveData(0)
    var progress3 = MutableLiveData(0)
    var progress4 = MutableLiveData(0)
    var progress5 = MutableLiveData(0)
    var gnssInfo = MutableLiveData("")
    var gpsLastSeen = MutableLiveData("7th Dec 00:49")
    var gpsSat = MutableLiveData("6")
    var gpsLat = MutableLiveData("0 N")
    var gpsLong = MutableLiveData("0 W")
    var gpsLatitude = MutableLiveData(51.5054)
    var gpsLongitude = MutableLiveData(0.0235)
    var gpsAlt = MutableLiveData("10m")
    var gpsSpeed = MutableLiveData("1m/s")

    var joystickEnable = MutableLiveData(false)

    var dummy = "+CGNSSINFO: 2,06,02,00,5130.517255,N,00001.892836,W,061221,232939.0,20.7,0.0,,1.5,1.2,0.9"

    fun gpsInfoListener(){
        databaseRef.child("GNSS").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(gnssInfoData: DataSnapshot) {
                extractGPSInfo(gnssInfoData)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun extractGPSInfo(gpsInfo:DataSnapshot){
        gnssInfo.value = gpsInfo.child("M").value.toString()
        val x = gnssInfo.value!!.split(",")

        gpsLastSeen.value = SimpleDateFormat("d MMM 'at' h:mm:ss a").format(SimpleDateFormat("ddMMyyHHmmss").parse(x[8]+x[9])).toString()
        gpsSat.value = (x[1].toInt() + x[2].toInt() + x[3].toInt()).toString()
        val lat = x[4].slice(0..1).toDouble() +   x[4].removeRange(0,2).toDouble()/60
        val long= x[6].slice(0..2).toDouble()+  x[6].removeRange(0,3).toDouble()/60

        when(x[5]){
            "N"->gpsLatitude.value = lat
            "S"->gpsLatitude.value = -lat
        }
        when(x[7]){
            "E"->gpsLongitude.value = long
            "W"->gpsLongitude.value = -long
        }

         gpsLat.value = String.format("%.8f",lat)  +"\u00B0"+" " + x[5]  //    "\u00B0" + x[4].slice()
        gpsLong.value = String.format("%.8f",long)  +"\u00B0"+" " + x[7]
        gpsAlt.value = x[10]+"m"
        gpsSpeed.value = String.format("%.1f", x[11].toDouble()*1.852)+" kmph"   //Knots to kmph = 1.852


        Log.d("GPSLog", " "+x[1]+" "+x[2]+" "+x[3]+" "+x[4]+" "+x[5] +" "+x[6]+" "+x[7]+" "+x[8]+" "+x[9])
        Log.d("GPSLog", gnssInfo.value!!)
    }
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

    fun resetESP(progressNo: Int){
        when(progressNo){
            1-> progress1.value=0
            2-> progress2.value=0
            3-> progress3.value=0
            4-> progress4.value=0
            5-> progress5.value=0
            6->{//Reset all to 0//
                for (i in 1..5){
                    resetESP(i)
                }
            }
        }
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