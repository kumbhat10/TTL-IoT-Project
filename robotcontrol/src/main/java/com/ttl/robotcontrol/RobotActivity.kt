package com.ttl.robotcontrol

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.airbnb.lottie.LottieDrawable
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
import kotlin.random.Random

class RobotActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityRobotBinding
    private lateinit var viewModel: RobotViewModel


    private lateinit var marker: Marker
    private lateinit var googleMap: GoogleMap
    private lateinit var databaseRef: DatabaseReference

    //    private lateinit var databaseRef1: DatabaseReference
    private val int_call_phone_request_code = 103
    private val int_sms_request_code = 104
    private val intReadPhoneNumber = 105

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
        viewModel.firmwareUpdateListener()
        viewModel.gpsInfoListener()
        viewModel.bvListener()
//        binding.gridControlStats!!.layoutManager = GridLayoutManager(this, 1)
//        binding.recyclerViewAdapter = RobotStatsGridAdapter(arrayList = setDataList())
        binding.mapViewRobot.onCreate(savedInstanceState)
        binding.mapViewRobot.getMapAsync(this)

        databaseRef = Firebase.database.getReference("Robot/Control/data")
        observeDataAndUpdateServer()
    }

    fun pickUpCallRobot(view: View) {
        databaseRef.updateChildren(mapOf("AT" to "ATA"))
    }

    fun hangUpCallRobot(view: View) {
        databaseRef.updateChildren(mapOf("AT" to "AT+cvhu=0"))
        Handler(Looper.getMainLooper()).postDelayed({ databaseRef.updateChildren(mapOf("AT" to "ATH")) }, 1000L)
    }

    @SuppressLint("MissingPermission")
    fun receiveCallRobot(view: View) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_NUMBERS), intReadPhoneNumber)
        } else {
            try {
                val telephoneManager =
                    getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val myPhoneNumber = telephoneManager.line1Number
                Toast.makeText(this, myPhoneNumber.substring(1), Toast.LENGTH_SHORT).show()
                databaseRef.updateChildren(mapOf("AT" to "ATD+44" + myPhoneNumber.substring(1) + ";"))
            } catch (_: Exception) {
                databaseRef.updateChildren(mapOf("AT" to "ATD+447496393966;"))
            }
        }
    }

    fun callRobot(view: View) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:" + getString(R.string.phone_no_robot))

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), int_call_phone_request_code)
        } else {
            try {
                startActivity(callIntent)
            } catch (me: Exception) {
                Toast.makeText(this, "Failed to make a call: " + me.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun smsRobot(view: View) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), int_sms_request_code)
        } else {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(getString(R.string.phone_no_robot), null, "Seed - " + Random.nextInt(1, 1000).toString() + " -> Hello from Android App", null, null)
            } catch (me: Exception) {
                Toast.makeText(this, "Failed to send SMS: " + me.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startExcavator(view: View) {
        startActivity(Intent(this, ExcavatorActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        overridePendingTransition(R.anim.slide_left_activity, R.anim.slide_left_activity)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            int_call_phone_request_code -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Phone call permission granted", Toast.LENGTH_SHORT).show()
                    callRobot(View(this))
                }
            }
            int_sms_request_code -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Send SMS permission granted", Toast.LENGTH_SHORT).show()
                    smsRobot(View(this))
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun observeDataAndUpdateServer() {
        viewModel.downlaodingFirmware.observe(this) {
            when {
                it -> {
                    binding.maskDownloading.visibility = View.VISIBLE
                    binding.downloadingText.visibility = View.VISIBLE
                    viewModel.downlaodingFirmwarePrev.value = true
                    binding.downloadLottie.scaleX = 2.6F
                    binding.downloadLottie.scaleY = 2.6F
                    binding.downloadLottie.setAnimation(R.raw.downloading)
                    binding.downloadLottie.repeatCount = LottieDrawable.INFINITE
                    binding.downloadLottie.playAnimation()
                    binding.downloading.text = getString(R.string.downloading_firmware_to_esp32)
                }
                viewModel.downlaodingFirmwarePrev.value == true -> {
                    viewModel.downlaodingFirmwarePrev.value = false
                    binding.downloadLottie.scaleX = 1.4F
                    binding.downloadLottie.scaleY = 1.4F
                    binding.downloadLottie.setAnimation(R.raw.done)
                    binding.downloadLottie.repeatCount = 0
                    binding.downloadLottie.playAnimation()
                    binding.downloading.text = getString(R.string.updatefirmware)
                    binding.downloadingText.visibility = View.GONE
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.maskDownloading.visibility = View.GONE
                    }, 3000)
                }
                else -> {
                    viewModel.downlaodingFirmwarePrev.value = false
                    binding.maskDownloading.visibility = View.GONE
                }
            }
        }

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
        viewModel.buzzerEnable.observe(this) {
            if (it) databaseRef.updateChildren(mapOf("bs" to 1))
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
        binding.mapViewRobot.onResume()
    }

    override fun onStart() { //removed API key from here
        super.onStart()
        binding.mapViewRobot.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapViewRobot.onStop()
    }

    override fun onPause() {
        binding.mapViewRobot.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapViewRobot.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }


}