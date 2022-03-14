package com.ttl.robotcontrol
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.airbnb.lottie.LottieDrawable
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ttl.robotcontrol.databinding.ActivityExcavatorBinding
import java.util.ArrayList
import kotlin.random.Random

class ExcavatorActivity : AppCompatActivity(), OnMapReadyCallback, View.OnTouchListener {
    private lateinit var binding: ActivityExcavatorBinding
    private lateinit var viewModel: ExcavatorViewModel
    private lateinit var marker: Marker
    private lateinit var googleMap: GoogleMap
    private lateinit var databaseRef: DatabaseReference
    private lateinit var aTDatabaseRef: DatabaseReference
    private lateinit var serialDatabaseRef: DatabaseReference
    private lateinit var serialTextAdapter: SerialTextRecyclerAdapter
    private var serialTextArray = ArrayList<SerialText>()
    private var serialTextData = MutableLiveData("")
    private var serialTextDataTemp = ""
    private var serialWindowOpen = false

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
    private val intCallPhoneRequestCode = 103
    private val intSMSRequestCode = 104
    private val intReadPhoneNumber = 105
    private val intUSBPermission = 106
    private val intLocationPermission = 107
    private lateinit var usbDevice: UsbDevice
    private lateinit var usbDeviceConnection: UsbDeviceConnection
    private lateinit var usbSerialDevice: UsbSerialDevice

    private lateinit var currentDeviceLocation: Location
    private val fusedLocationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var cancellationTokenSource = CancellationTokenSource()

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
        viewModel.firmwareUpdateListener()
        viewModel.gpsInfoListener()
        viewModel.bvListener()
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        binding.serialTextRecyclerView!!.layoutManager = LinearLayoutManager(this)
        serialTextAdapter = SerialTextRecyclerAdapter(serialTextArray)
        binding.serialTextRecyclerView!!.adapter = serialTextAdapter

        databaseRef = Firebase.database.getReference("Excavator/Control/data")
        aTDatabaseRef = Firebase.database.getReference("Excavator/AT")
        serialDatabaseRef = Firebase.database.getReference("Serial")
        binding.editTextChatInput!!.setOnEditorActionListener { v, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendSerialCommand(v)
                    hideKeyboard()
                    false
                }
                else -> false
            }
        }

        handler = Handler(Looper.getMainLooper())
        updateFirebaseTask = object : Runnable {
            override fun run() {
                writeToFirebase()
                handler.postDelayed(this, refreshTimer)
            }
        }

        observeDataAndUpdateServer()
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        for (dev in deviceList){
            if(dev.value.vendorId == 6790){
                usbDevice = dev.value
                usbDeviceConnection = usbManager.openDevice(usbDevice)
                usbAdapter1()
            }
        }
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
//            Toast.makeText(this, "Requesting currentDeviceLocation now",Toast.LENGTH_SHORT).show()
            val currentLocationTask = fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            currentLocationTask.addOnCompleteListener{task->
                if(task.isSuccessful){
                    currentDeviceLocation = task.result
                    val latLong = LatLng(currentDeviceLocation.latitude, currentDeviceLocation.longitude)
                    val marker = googleMap.addMarker(MarkerOptions().position(latLong).title("Phone").visible(true))!!
                    marker.showInfoWindow()
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
                }
            }
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), intLocationPermission)
        }
    }

    fun openCloseSerialWindow(view: View){
        if(serialWindowOpen) {
            binding.serialWindow!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.zoomout_scoretable_close))
            Handler(Looper.getMainLooper()).postDelayed({binding.serialWindow!!.visibility = View.GONE}, 240)
        }
        else{
            binding.serialWindow!!.visibility = View.VISIBLE
            binding.serialWindow!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.zoomin_scoretable_open))
        }
        serialWindowOpen = !serialWindowOpen
    }

    @SuppressLint("SetTextI18n")
    private fun usbAdapter1(){

        usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbDeviceConnection)
        usbSerialDevice.open();
        usbSerialDevice.setBaudRate(115200);
        usbSerialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
        usbSerialDevice.setParity(UsbSerialInterface.PARITY_NONE);
        usbSerialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

        val mCallBack = UsbSerialInterface.UsbReadCallback { data ->
            serialTextData.postValue(data.toString(Charsets.UTF_8))
        }
        usbSerialDevice.read(mCallBack)
    }

    fun pickUpCall(view: View){
        databaseRef.updateChildren(mapOf("AT" to "ATA"))
    }

    fun hangUpCall(view: View){
        databaseRef.updateChildren(mapOf("AT" to "AT+cvhu=0"))
        Handler(Looper.getMainLooper()).postDelayed({ databaseRef.updateChildren(mapOf("AT" to "ATH")) }, 1000L)
    }

    @SuppressLint("MissingPermission")
    fun receiveCallExcavator(view: View){
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

    fun callExcavator(view: View){
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:" + getString(R.string.phone_no_excavator))
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), intCallPhoneRequestCode)
        } else {
            try {
                startActivity(callIntent)
            } catch (me: Exception) {
                Toast.makeText(this, "Failed to make a call: " + me.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun smsExcavator(view: View){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), intSMSRequestCode)
        } else {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(getString(R.string.phone_no_excavator), null, "Seed - " + Random.nextInt(1, 1000).toString() + " -> Hello from Android App", null, null)
            } catch (me: Exception) {
                Toast.makeText(this, "Failed to send SMS: " + me.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        when (requestCode) {
            intLocationPermission -> {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                requestCurrentLocation()
            }
            intCallPhoneRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Phone call permission granted", Toast.LENGTH_SHORT).show()
                    callExcavator(View(this))
                }
            }
            intSMSRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Send SMS permission granted", Toast.LENGTH_SHORT).show()
                    smsExcavator(View(this))
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun writeToFirebase(){
        databaseRef.updateChildren(mapOf(
            "z1" to zValue(z1), "z2" to zValue(z2),
            "z3" to zValue(z3), "z4" to zValue(z4), "z6" to zValue(z6),
            "z7" to zValue(z7), "z8" to zValue(z8),
        ))
    }

    fun startRobot(view: View){
        startActivity(Intent(this, RobotActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        overridePendingTransition(R.anim.slide_left_activity, R.anim.slide_left_activity)
        finishAndRemoveTask()
    }

    private fun updateSerialData(serialText: String, received:Boolean = true){
        serialTextArray.add(SerialText(serialText, received))
        serialTextAdapter.notifyItemInserted(serialTextArray.size - 1)
        binding.serialTextRecyclerView!!.scrollToPosition(serialTextArray.size - 1)
    }

    fun sendSerialCommand(view: View){
        if(binding.editTextChatInput!!.text.toString().isNotEmpty()){
            updateSerialData("Send : " + binding.editTextChatInput!!.text.toString(), false)
            if(this::usbSerialDevice.isInitialized) usbSerialDevice.write(binding.editTextChatInput!!.text.toString().toByteArray())
            binding.editTextChatInput!!.setText("")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeDataAndUpdateServer(){
        serialTextData.observe(this) {
            binding.serialText!!.text = it
//            val key = serialDatabaseRef.child("R").push().key
//            serialDatabaseRef.child("R").updateChildren(mapOf(key to it))

            serialTextDataTemp += it
            if(it.contains("\n")) {
                updateSerialData(serialTextDataTemp.replace("\n",""))
                serialTextDataTemp = ""
            }

        }

        viewModel.downlaodingFirmware.observe(this){
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
            MotionEvent.actionToString(event!!.action) //+ "  " + v!!.id.toString()
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (v!!.id) {
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
                    z2 = 1  //base counter clock wise rotate
                }
                R.id.moveRight1 -> {
                    z2 = -1  //base clock wise rotate
                }
                R.id.moveCenter -> {
                    z4 = 1 // bucket grab
                }
                R.id.moveCenter1 -> {
                    z4 = -1 //bucket release
                }
            }
            writeToFirebase()
        }
        if (event.action == MotionEvent.ACTION_UP) { // move forward /backward
            if (v!!.id == R.id.moveLeft || v.id == R.id.moveRight || v.id == R.id.moveDown || v.id == R.id.moveUp) {
                z1 = 0
                z8 = 0
            }
            if (v.id == R.id.moveCenter1 || v.id == R.id.moveCenter) { //bucket grab/ release
                z4 = 0
            }
            if (v.id == R.id.moveLeft1 || v.id == R.id.moveRight1) {//base rotate
                z2 = 0
            }
            if (v.id == R.id.moveUp1 || v.id == R.id.moveDown1) { // main arm
                z7 = 0
            }
            if (v.id == R.id.moveUp2 || v.id == R.id.moveDown2) { //2nd arm
                z3 = 0
            }
            if (v.id == R.id.moveUp3 || v.id == R.id.moveDown3) { //bucket angle
                z6 = 0
            }
            writeToFirebase()
        }
        return false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        val latLong = LatLng(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!)
        marker = googleMap.addMarker(MarkerOptions().position(latLong).title("Excavator").visible(true))!!
        marker.showInfoWindow()
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null) {
            view?.let { v ->
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() { //removed API key from here
        super.onStart()
        handler.post(updateFirebaseTask)
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null);
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

    override fun onBackPressed() {
        this.moveTaskToBack(true)
    }
}