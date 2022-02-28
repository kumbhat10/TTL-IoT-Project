package com.ttl.robotcontrol

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ttl.robotcontrol.databinding.ActivityExcavatorBinding

class ExcavatorActivity : AppCompatActivity(), OnMapReadyCallback, View.OnTouchListener {
    private lateinit var binding: ActivityExcavatorBinding
    private lateinit var viewModel: ExcavatorViewModel
    private lateinit var marker: Marker
    private lateinit var googleMap: GoogleMap
    private lateinit var databaseRef: DatabaseReference
    private lateinit var updateFirebaseTask: Runnable
    private lateinit var handler: Handler
    private var z1 = 0
    private var z2 = 0
    private var z3 = 0
    private var z4 = 0
    private var z6 = 0
    private var z7 = 0
    private var z8 = 0
    private var refreshTimer = 100L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(true) //default: true
            .showErrorDetails(true) //default: true
            .showRestartButton(true) //default: true
            .logErrorOnRestart(false) //default: true
            .trackActivities(false) //default: false
            .errorDrawable(R.drawable.bug_icon) //default: bug image
            .apply()

        binding = ActivityExcavatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this);

        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this)[ExcavatorViewModel::class.java]
        binding.viewModel1 = viewModel // bind view model in XML layout to our viewModel
        viewModel.gpsInfoListener()
        viewModel.bvListener()
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        databaseRef = Firebase.database.getReference("Excavator/Control/data")

        handler = Handler(Looper.getMainLooper())
        updateFirebaseTask = object : Runnable {
            override fun run() {
                writeToFirebase()
                handler.postDelayed(this, refreshTimer)
            }
        }
        handler.post(updateFirebaseTask)
        observeDataAndUpdateServer()
    }

    private fun writeToFirebase(){
        databaseRef.updateChildren(mapOf(
            "z1" to zValue(z1), "z2" to zValue(z2),
            "z3" to zValue(z3), "z4" to zValue(z4), "z6" to zValue(z6),
            "z7" to zValue(z7), "z8" to zValue(z8),
        ))
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun observeDataAndUpdateServer() {
        viewModel.gnssInfo.observe(this) {
            val latLong = LatLng(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!)
            if (this::marker.isInitialized) marker.position = latLong
            if (this::googleMap.isInitialized) googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
        }
        viewModel.buzzerEnable.observe(this) {
            if (it) databaseRef.updateChildren(mapOf("bs" to 1))
            else databaseRef.updateChildren(mapOf("bs" to 0))
        }
        viewModel.power.observe(this) {
            if (it) databaseRef.updateChildren(mapOf("pwr" to 1))
            else databaseRef.updateChildren(mapOf("pwr" to 0))
        }
        binding.moveLeft.setOnTouchListener(this)
        binding.moveRight.setOnTouchListener(this)
        binding.moveUp.setOnTouchListener(this)
        binding.moveDown.setOnTouchListener(this)

        binding.moveUp1.setOnTouchListener(this)
        binding.moveDown1.setOnTouchListener(this)

        binding.moveUp2.setOnTouchListener(this)
        binding.moveDown2.setOnTouchListener(this)

        binding.moveUp3.setOnTouchListener(this)
        binding.moveDown3.setOnTouchListener(this)

        binding.moveCenter.setOnTouchListener(this)
        binding.moveCenter1.setOnTouchListener(this)
        binding.moveLeft1.setOnTouchListener(this)
        binding.moveRight1.setOnTouchListener(this)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        binding.debugText.text =
            MotionEvent.actionToString(event!!.action) + "  " + v!!.id.toString()
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (v.id) {
                R.id.moveLeft -> {
                    z1 = 1 //backward
                    z8 = -1 //forward
                }
                R.id.moveRight -> {
                    z1 = -1 //forward
                    z8 = 1 //backward
                }
                R.id.moveUp -> {
                    z1 = -1 //forward
                    z8 = -1 //forward
                }
                R.id.moveDown -> {
                    z1 = 1 //backward
                    z8 = 1 //backward
                }
                R.id.moveUp1 -> {// main arm
                    z7 = -1
                }
                R.id.moveDown1 -> {// main arm
                    z7 = 1
                }
                R.id.moveUp2 -> { //second arm 2
                    z3 = 1
                }
                R.id.moveDown2 -> {//second arm 2
                    z3 = -1
                }
                R.id.moveUp3 -> { //bucket angle
                    z6 = -1
                }
                R.id.moveDown3 -> {//bucket angle
                    z6 = 1
                }
                R.id.moveLeft1 -> {
                    z2 = 1  //counter clock wise rotate
                }
                R.id.moveRight1 -> {
                    z2 = -1  //clock wise rotate
                }
                R.id.moveCenter->{
                    z4 = 1 // bucket grab
                }
                R.id.moveCenter1->{
                    z4 = -1 //bucket release
                }


            }
            writeToFirebase()
        }
        if (event.action == MotionEvent.ACTION_UP) { // move forward /backward
                if(v.id ==R.id.moveLeft || v.id ==R.id.moveRight ||  v.id ==R.id.moveDown||v.id ==R.id.moveUp) {
                    z1 = 0
                    z8=0
                }
            if(v.id ==R.id.moveCenter1 || v.id ==R.id.moveCenter ) { //bucket grab/ release
                z4=0
                }
            if(v.id ==R.id.moveLeft1 || v.id ==R.id.moveRight1 ) {//base rotate
                    z2 = 0
                }
            if(v.id ==R.id.moveUp1 || v.id ==R.id.moveDown1 ) { // main arm
                    z7 = 0
                }
            if(v.id ==R.id.moveUp2 || v.id ==R.id.moveDown2 ) { //2nd arm
                    z3 = 0
                }
            if(v.id ==R.id.moveUp3 || v.id ==R.id.moveDown3 ) { //bucket angle
                    z6 = 0
                }
            writeToFirebase()
        }
        return false
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        val latLong = LatLng(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!)
        marker =
            googleMap.addMarker(MarkerOptions().position(latLong).title("Excavator").visible(true))!!
        marker.showInfoWindow()
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() { //removed API key from here
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
        super.onDestroy()
    }


}