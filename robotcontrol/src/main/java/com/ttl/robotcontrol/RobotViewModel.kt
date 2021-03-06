@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.ttl.robotcontrol

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import kotlin.random.Random

class RobotViewModel : ViewModel() {

    private val databaseRef = Firebase.database.getReference("Robot/Control")
    var count = 0
    var reportCrash = true
    var reportCrashRefresh = 2*1000L
    var handler = android.os.Handler(Looper.getMainLooper())

    var progress1 = MutableLiveData(FirebaseData().pro_lx) //lx
    var progress2 = MutableLiveData(FirebaseData().pro_ly)//ly
    var progress3 = MutableLiveData(FirebaseData().pro_ry)//ry
    var progress4 = MutableLiveData(FirebaseData().pro_rx)//rx
    var progress5 = MutableLiveData(FirebaseData().pro_tr)//tr

    var gnssInfo = MutableLiveData("")
    var gpsLastSeen = MutableLiveData("7th Dec 00:49")
    var gpsSat = MutableLiveData("6")
    var gpsLat = MutableLiveData("0 N")
    var gpsLong = MutableLiveData("0 W")
    var gpsLatitude = MutableLiveData(52.276968) // MutableLiveData(51.50860150)  // EIDC ->  52.276968
    var gpsLongitude = MutableLiveData(-1.549007) // MutableLiveData(0.03153882)   //  EIDC -> -1.549007
//    // var gpsLatitude = MutableLiveData(51.5054)
//    var gpsLongitude = MutableLiveData(0.0235)
    var gpsAlt = MutableLiveData("10m")
    var gpsSpeed = MutableLiveData("1m/s")
    var bv = MutableLiveData("7.00v")
    var bvInfo = MutableLiveData("Last updated ")

    var joystickEnable = MutableLiveData(false)
    var buzzerEnable = MutableLiveData(true)
    var danceEnable = MutableLiveData(false)
    var downlaodingFirmware = MutableLiveData(false)
    var downlaodingFirmwarePrev = MutableLiveData(false)

    fun buzzOnce() {
        if(buzzerEnable.value!!) Firebase.database.getReference("Robot/Control/data").updateChildren(mapOf("Buzz" to Random.nextInt(0, 1000)))
    }

    fun firmwareUpdateListener(){
        Firebase.database.getReference("Robot/AT/Update").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(data: DataSnapshot) {
                if (data.exists() || data.value != null) {
                    downlaodingFirmware.value = data.value==1L
                }
            }
            override fun onCancelled(error: DatabaseError) {            }
        })
    }
    fun gpsInfoListener() {
        databaseRef.child("GNSS").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(gnssInfoData: DataSnapshot) {
                if(gnssInfoData.exists() || gnssInfoData.value != null)
                {
                    try {
                        extractGPSInfo(gnssInfoData)
                    }catch (me:Exception){

                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun bvListener() {
        Firebase.database.getReference("Robot/BatteryVoltage").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(bvData: DataSnapshot) {
               if(bvData.exists() || bvData.value !=null){
                   val bvTemp = bvData.child("V").value.toString().toDouble()
                   if (bvTemp > 0.5) {
                       bv.value = (String.format("%.3f", bvTemp) + "v")
                   } else {
                       if(reportCrash) batteryDisconnectedReport(bv.value!!)
                       bv.value = "Disconnected"
                   }
                   try {
                       bvInfo.value = "Last update : \n" +
                               SimpleDateFormat("d MMM 'at' h:mm:ss a").format(SimpleDateFormat("yyyyMMddHH:mm:ss").parse(
                                   bvData.child("D").value!!.toString() + bvData.child("T").value!!.toString()
                               )).toString()
                   }catch(me:Exception){
                       bvInfo.value = "Last update : \nDate & Time not available"

                   }
               }else
               {
                   bv.value = "Server error"
               }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    @SuppressLint("SimpleDateFormat")
    fun extractGPSInfo(gpsInfo: DataSnapshot) {
//        "+CGNSSINFO: 2,06,02,00,5130.517255,N,00001.892836,W,081221,024939.0,20.7,0.0,,1.5,1.2,0.9"
        gnssInfo.value = gpsInfo.child("M").value.toString()
        val x = gnssInfo.value!!.split(",")
        try {
            gpsLastSeen.value =
                SimpleDateFormat("d MMM 'at' h:mm:ss a").format(SimpleDateFormat("ddMMyyHHmmss").parse(x[8] + x[9])).toString()
        }catch(me:Exception){
            gpsLastSeen.value = "Date & Time not available"
        }
        gpsSat.value = (x[1].toInt() + x[2].toInt() + x[3].toInt()).toString()
        val lat = x[4].slice(0..1).toDouble() + x[4].removeRange(0, 2).toDouble() / 60
        val long = x[6].slice(0..2).toDouble() + x[6].removeRange(0, 3).toDouble() / 60

        when (x[5]) {
            "N" -> gpsLatitude.value = lat
            "S" -> gpsLatitude.value = -lat
        }
        when (x[7]) {
            "E" -> gpsLongitude.value = long
            "W" -> gpsLongitude.value = -long
        }
        gpsLat.value =
            String.format("%.8f", lat) + "\u00B0" + " " + x[5]  //    "\u00B0" + x[4].slice()
        gpsLong.value = String.format("%.8f", long) + "\u00B0" + " " + x[7]
        gpsAlt.value = x[10] + "m"
        gpsSpeed.value =
            String.format("%.1f", x[11].toDouble() * 1.852) + " kmph"   //Knots to kmph = 1.852
//        Log.d("GPSLog", " "+x[1]+" "+x[2]+" "+x[3]+" "+x[4]+" "+x[5] +" "+x[6]+" "+x[7]+" "+x[8]+" "+x[9])
//        Log.d("GPSLog", gnssInfo.value!!)
    }

    fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        val lx = angleFromJoystick(event!!.x)
        val ly = angleFromJoystick(event.y)
        val ry = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_RZ))
        val rx = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_Z))
        val tr = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_RTRIGGER))
        val tl = angleFromJoystick(event.getAxisValue(MotionEvent.AXIS_LTRIGGER))
        val trigger = (tr - tl) + 315

        progress1.value = lx
        progress2.value = ly
        progress3.value = ry
        progress4.value = rx
        progress5.value = trigger

        count += 1
        Log.d("ViewModel", "Sending Again  $count")
//        databaseRef.setValue(FirebaseData(lx, ly, rx, ry, trigger))
        return true //super.onGenericMotionEvent(event)
    }

    fun fakeCrash() {
        throw RuntimeException("Test Crash") // Force a crash
    }

    fun batteryDisconnectedReport(voltage:String) {
        reportCrash = false
        try {
            throw RuntimeException("Robot Battery was Disconnected - Fake Crash")
        } catch (e: Exception) {
            Firebase.crashlytics.log(" Last known voltage of robot was $voltage")
            Firebase.crashlytics.recordException(e)
        }
        handler.postDelayed({reportCrash=true},reportCrashRefresh)
    }

    fun resetESP(progressNo: Int) {
        when (progressNo) {
            1 -> progress1.value = 0
            2 -> progress2.value = 0
            3 -> progress3.value = 0
            4 -> progress4.value = 0
            5 -> progress5.value = 0
            6 -> {//Reset all to 0//
                for (i in 1..5) {
                    resetESP(i)
                }
            }
        }
    }

    fun defaultPosition() {
        progress1.value = FirebaseData().pro_lx
        progress2.value = FirebaseData().pro_ly
        progress3.value = FirebaseData().pro_ry
        progress4.value = FirebaseData().pro_rx
        progress5.value = FirebaseData().pro_tr
    }

    fun standUpPosition() {
        progress1.value = standUp().pro_lx
        progress2.value = standUp().pro_ly
        progress3.value = standUp().pro_ry
        progress4.value = standUp().pro_rx
        progress5.value = standUp().pro_tr
    }
    fun pose1Set() {
        progress1.value = pose1().pro_lx
        progress2.value = pose1().pro_ly
        progress3.value = pose1().pro_ry
        progress4.value = pose1().pro_rx
        progress5.value = pose1().pro_tr
    }
    fun pose2Set() {
        progress1.value = pose2().pro_lx
        progress2.value = pose2().pro_ly
        progress3.value = pose2().pro_ry
        progress4.value = pose2().pro_rx
        progress5.value = pose2().pro_tr
    }
    fun gripClose() {

        progress5.value = 180
    }
    fun gripOpen() {

        progress5.value = -180
    }

    private fun angleFromJoystick(x: Float): Int {
        return (((x * 100).roundToInt()) + 315).coerceIn(90, 540)
    }

}