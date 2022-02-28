package com.ttl.robotcontrol

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ttl.robotcontrol.databinding.ActivityRobotBinding
import kotlin.math.round

class RobotActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityRobotBinding
    private lateinit var viewModel: RobotViewModel


    private lateinit var marker: Marker
    private lateinit var googleMap: GoogleMap
    private lateinit var databaseRef: DatabaseReference
//    private lateinit var databaseRef1: DatabaseReference

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
        FirebaseApp.initializeApp(this);

        binding = ActivityRobotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this)[RobotViewModel::class.java]
        binding.viewModel1 = viewModel // bind view model in XML layout to our viewModel
        viewModel.gpsInfoListener()
        viewModel.bvListener()
//        binding.gridControlStats!!.layoutManager = GridLayoutManager(this, 1)
//        binding.recyclerViewAdapter = RobotStatsGridAdapter(arrayList = setDataList())
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        databaseRef = Firebase.database.getReference("Robot/Control/data")
        observeDataAndUpdateServer()
    }

    private fun observeDataAndUpdateServer() {
        viewModel.progress1.observe(this) {
            binding.t11.text = (round((it / 2).toDouble()).toInt()).toString()
            databaseRef.updateChildren(mapOf("lx" to Servo().getPWM("lx", it = it)))
            binding.t11.text = (round((it / 2).toDouble()).toInt()).toString()
        }
        viewModel.progress2.observe(this) {
            databaseRef.updateChildren(mapOf("ly" to Servo().getPWM("ly", it = it)))
            binding.t22.text = (round((it / 2).toDouble()).toInt()).toString()
        }
        viewModel.progress3.observe(this) {
            databaseRef.updateChildren(mapOf("ry" to Servo().getPWM("ry", it = it)))
            binding.t33.text = (round((it / 2).toDouble()).toInt()).toString()
        }
        viewModel.progress4.observe(this) {
            databaseRef.updateChildren(mapOf("rx" to Servo().getPWM("rx", it = it)))
            binding.t44.text = (round((it / 2).toDouble()).toInt()).toString()

        }
        viewModel.progress5.observe(this) {
            databaseRef.updateChildren(mapOf("tr" to Servo().getPWM("tr", it = it)))
            binding.t55.text = (round((it / 2).toDouble()).toInt()).toString()
        }

        viewModel.gnssInfo.observe(this) {
            val latLong = LatLng(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!)
            if (this::marker.isInitialized) marker.position = latLong
            if (this::googleMap.isInitialized) googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
        }
        viewModel.buzzerEnable.observe(this){
            if(it) databaseRef.updateChildren(mapOf("bs" to 1))
            else databaseRef.updateChildren(mapOf("bs" to 0))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (viewModel.joystickEnable.value == true) viewModel.onGenericMotionEvent(event)
        return super.onGenericMotionEvent(event)
    }

    override fun onMapReady(googleMap: GoogleMap) {
//        toastCenter("Google maps API are ready now")
        this.googleMap = googleMap
        val latLong = LatLng(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!)
        marker =
            googleMap.addMarker(MarkerOptions().position(latLong).title("IoT Robot").visible(true))!!
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