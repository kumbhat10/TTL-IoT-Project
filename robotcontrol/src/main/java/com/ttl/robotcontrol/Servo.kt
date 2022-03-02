package com.ttl.robotcontrol

import android.view.MotionEvent
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class Servo(val resetPositionAngle: Int = 315) {
//    private var servoMin = 90
//    private var servoMax = 540
    // base - full(speed 5-20), s=15 arm1 10 down:up , arm2 - 490max up:down,s-10, gripperR - s=5 acw:cw, grip: 185:355open:close s=5

    private fun getPWMFromProgress(
        progress: Int,
        minPWM: Int,
        maxPWM: Int,
        minProgress: Double = (-200).toDouble(),
        maxProgress: Double = (200).toDouble()
    ): Int {
        val slope = (maxPWM - minPWM) / (maxProgress - minProgress)
        return (minPWM + (slope * (progress - minProgress))).toInt().coerceIn(minPWM, maxPWM)
    }

    fun getPWM(servo: String, it: Int): Int {
        return when (servo) {
            "lx" -> 540 - getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 540) + 90
            "ly" -> getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 400)
            "ry" -> getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 490)
            "rx" -> getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 540)
            "tr" -> getPWMFromProgress(progress = it, minPWM = 185, maxPWM = 355)
            else -> getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 540)
        }
    }
}
class FirebaseData(
    val pro_lx: Int = 24,  //base
    val pro_ly: Int = 170, //main arm
    val pro_rx: Int = -10, //rotate griper
    val pro_ry: Int = 190, //2nd arm
    val pro_tr: Int = 0, //gripper
) {
}
fun resetPosition() {
    Firebase.database.getReference("Robot/Control/data").updateChildren(mapOf(
        "lx" to Servo().getPWM("lx", it = FirebaseData().pro_lx), //base
        "ly" to Servo().getPWM("ly", it = FirebaseData().pro_ly), //main arm
        "ry" to Servo().getPWM("ry", it = FirebaseData().pro_ry), //second arm
        "rx" to Servo().getPWM("rx", it = FirebaseData().pro_rx), // Gripper Rotate
        "tr" to Servo().getPWM("tr", it = FirebaseData().pro_tr), // Gripper
    ))
}
fun pose1(): FirebaseData {
    return FirebaseData(200, 68, -10,  -92, 0)
}
fun pose2(): FirebaseData {
    return FirebaseData(-110, 68, -10,  -92, 0)
}
fun standUp(): FirebaseData {
    return FirebaseData(24, 68, -10,  -92, 0)
}

fun pos(): Int{
    return Random.nextInt(1, 1000)
}
fun neg(): Int{
    return -1*Random.nextInt(1, 1000)
}
fun zValue(i:Int):Int{
    return if(i==0) 0
    else if(i>0) pos()
    else neg()
}

