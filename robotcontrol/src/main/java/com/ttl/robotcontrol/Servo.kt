package com.ttl.robotcontrol

import androidx.lifecycle.MutableLiveData

class Servo(val resetPosition:Int = 90) {
    var angle = MutableLiveData(90)
    var speed = MutableLiveData(0)
}

class FirebaseData(    val lx: Int = 90,
                       val ly: Int = 90,
                       val rx: Int = 90,
                       val ry: Int = 90,
                       val tr: Int = 90,
                       val rt: Int  = 0) {

}