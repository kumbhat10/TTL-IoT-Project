package com.ttl.robotcontrol

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.ttl.robotcontrol.databinding.ActivityFullscreenBinding
import kotlin.math.round

class FullscreenActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var viewModel: FullscreenViewModel
    private var precision = 3
    private var maxDegree = 180
    private var lx = Servo().resetPositionAngle
    private var ly = Servo().resetPositionAngle
    private var rx = Servo().resetPositionAngle
    private var ry = Servo().resetPositionAngle
    private var tr = Servo().resetPositionAngle

    private lateinit var databaseRef:DatabaseReference
    private lateinit var databaseRef1:DatabaseReference

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

        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this)[FullscreenViewModel::class.java]
        binding.viewModel1 = viewModel // bind view model in XML layout to our viewModel

//        binding.gridControlStats!!.layoutManager = GridLayoutManager(this, 1)
//        binding.recyclerViewAdapter = RobotStatsGridAdapter(arrayList = setDataList())
        binding.mapView?.onCreate(savedInstanceState)
        binding.mapView?.getMapAsync(this)

        databaseRef =  Firebase.database.getReference("Control/data")
        databaseRef1 =  Firebase.database.getReference("Control/dataProgress")
        observeDataAndUpdateServer()
    }

    private fun observeDataAndUpdateServer(){
        viewModel.progress1.observe(this) {
            binding.t11!!.text = (round((it/2).toDouble()).toInt()).toString()
            lx= Servo().getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 540)
            databaseRef.setValue(FirebaseData(lx=lx, ly=ly, rx=rx, ry=ry, tr=tr))
//            databaseRef1.setValue(FirebaseData(lx=viewModel.progress1.value!!,
//                ly=viewModel.progress2.value!!, rx=viewModel.progress4.value!!, ry=viewModel.progress3.value!!, tr=viewModel.progress5.value!!))
        }
        viewModel.progress2.observe(this) {
            ly= Servo().getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 400)
            databaseRef.setValue(FirebaseData(lx=lx, ly=ly, rx=rx, ry=ry, tr=tr))
            binding.t22!!.text = (round((it/2).toDouble()).toInt()).toString()
        }
        viewModel.progress3.observe(this) {
            ry= Servo().getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 490)
            databaseRef.setValue(FirebaseData(lx=lx, ly=ly, rx=rx, ry=ry, tr=tr))
            binding.t33!!.text = (round((it/2).toDouble()).toInt()).toString()
        }
        viewModel.progress4.observe(this) {
            rx= Servo().getPWMFromProgress(progress = it, minPWM = 90, maxPWM = 540)
            databaseRef.setValue(FirebaseData(lx=lx, ly=ly, rx=rx, ry=ry, tr=tr))
            binding.t44!!.text = (round((it/2).toDouble()).toInt()).toString()

        }
        viewModel.progress5.observe(this) {
            tr= Servo().getPWMFromProgress(progress = it, minPWM = 185, maxPWM = 355)
            databaseRef.setValue(FirebaseData(lx=lx, ly=ly, rx=rx, ry=ry, tr=tr))
//            Log.d("Progress", "tr = $tr")
            binding.t55!!.text = (round((it/2).toDouble()).toInt()).toString() }

//        databaseRef1.addValueEventListener(object : ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//            val data = snapshot.getValue<FirebaseData>()!!
//                viewModel.progress1.value = data.lx
//                viewModel.progress2.value = data.ly
//                viewModel.progress3.value = data.ry
//                viewModel.progress4.value = data.rx
//                viewModel.progress5.value = data.tr
//            }
//            override fun onCancelled(error: DatabaseError) {
//            }
//        })
    }

    @SuppressLint("SetTextI18n")
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if(viewModel.joystickEnable.value == true) viewModel.onGenericMotionEvent(event)
        return super.onGenericMotionEvent(event)
    }

    private fun toastCenter(message: String) {
        Snackbar.make(binding.mainLayout, message, Snackbar.LENGTH_LONG).setAction("Dismiss") {}
            .setActionTextColor(getColor(R.color.borderblue)).show()
    }

    private val robotIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.font_yellow)
        BitmapHelper.vectorToBitmap(this, R.drawable.android_24, color)
    }

    override fun onMapReady(googleMap: GoogleMap) {
//        toastCenter("Google maps API are ready now")
        val latLong = LatLng(51.5054, 0.0235)
        googleMap.addMarker(MarkerOptions().position(latLong).title("IoT Robot").visible(true))
        googleMap.addMarker(MarkerOptions().position(latLong).title("IoT Robot").visible(true))
            ?.showInfoWindow()
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
    }

    override fun onResume() {
        super.onResume()
        binding.mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView?.onStart() //AIzaSyAYPJUzpcLQooXZXK6PmcTyGxcHgl1Dr7w
    }

    override fun onStop() {
        super.onStop()
        binding.mapView?.onStop()
    }
    override fun onPause() {
        binding.mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapView?.onDestroy()
        super.onDestroy()
    }


}