package com.ttl.robotcontrol

import androidx.lifecycle.MutableLiveData

class Servo(val resetPositionAngle: Int = 315) {
    var angle = MutableLiveData(90)
    var speed = MutableLiveData(0)
    var rangeMin = MutableLiveData(0)
    var rangeMax = MutableLiveData(0)

    private var servoMin = 90
    private var servoMax = 540
    // base - full(speed 5-20), s=15 arm1 10 down:up , arm2 - 490max up:down,s-10, gripperR - s=5 acw:cw, grip: 185:355open:close s=5

    fun getPWMFromProgress(
        progress: Int,
        minPWM: Int,
        maxPWM: Int,
        minProgress: Double = (-200).toDouble(),
        maxProgress: Double = (200).toDouble()
    ): Int {
        val slope = (maxPWM - minPWM) / (maxProgress - minProgress)
        return (minPWM +  (slope * (progress - minProgress))).toInt().coerceIn(minPWM, maxPWM)
    }

}

class FirebaseData(
    val lx: Int = Servo().resetPositionAngle,
    val ly: Int = 385,
    val rx: Int = 310,
    val ry: Int = 490,
    val tr: Int = 270,
    val rt: Int = 0) {

    fun pose1():FirebaseData{
        return FirebaseData(184,389,309,411,270,0)
    }
    fun pose2():FirebaseData{
        return FirebaseData(446,389,309,411,270,0)
    }

}