package com.ttl.robotcontrol

//import android.bluetooth.BluetoothManager
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.location.Location
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.harrysoft.androidbluetoothserial.BluetoothManager
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface
import com.ttl.robotcontrol.databinding.ActivityExcavatorBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.random.Random


class ExcavatorActivity : AppCompatActivity(), OnMapReadyCallback, View.OnTouchListener {
    private lateinit var binding: ActivityExcavatorBinding
    private lateinit var viewModel: ExcavatorViewModel
    private lateinit var markerExcavator: Marker
    private lateinit var markerDevice: Marker
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
    private val intBluetoothPermission = 108
    private var bluetoothEnablingActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this@ExcavatorActivity, "Activity Result ok ${result.data}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@ExcavatorActivity, "Activity Result failed ${result.data}", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var usbDevice: UsbDevice
    private lateinit var usbDeviceConnection: UsbDeviceConnection
    private lateinit var usbSerialDevice: UsbSerialDevice
    private lateinit var bluetoothManagerSerial: BluetoothManager
    private val espBluetoothMacAddress = "34:86:5D:5E:75:0E"
    private lateinit var deviceInterface: SimpleBluetoothDeviceInterface
    private lateinit var currentDeviceLocation: Location
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var cancellationTokenSource = CancellationTokenSource()
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var speechRecognizeEnabled = false
    private var mAudioManager: AudioManager? = null
    private var mStreamVolume = 0
    private val timerVoiceAction = 1500L
    private val timerVoiceActionLow = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
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


        registerBroadcastReceiver() //        val a = MediaPlayer.create(this, R.raw.success)
        //        a.prepareAsync()
        //        a.start()

        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this)[ExcavatorViewModel::class.java]
        binding.viewModel1 = viewModel // bind view model in XML layout to our viewModel
        viewModel.firmwareUpdateListener()
        viewModel.gpsInfoListener()
        viewModel.bvListener()
        binding.mapViewExcavator.onCreate(savedInstanceState)
        binding.mapViewExcavator.getMapAsync(this)
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
        for (dev in deviceList) {
            if (dev.value.vendorId == 6790) {
                usbDevice = dev.value
                usbDeviceConnection = usbManager.openDevice(usbDevice)
                usbAdapter1()
            }
        } //        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermissions()
    }

    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) //        intentFilter.addAction(BluetoothDevice.ACTION_UUID)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        val mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val actionString = intent!!.action.toString().removePrefix("android.bluetooth.device.action.") //                Toast.makeText(this@ExcavatorActivity, "broadcast Receiver : $actionString", Toast.LENGTH_SHORT).show()
                Log.d("bluetooth", "broadcast Receiver : $actionString ${intent.dataString} ${intent.clipData} ${intent.data} ${intent.component}")
                updateSerialData(serialText = "Broadcast Receiver : ${actionString}\n", getColor(R.color.font_yellow))
            }
        }
        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    fun speechRecognitionEnable(view: View) {
        speechRecognizeEnabled = !speechRecognizeEnabled
        if (speechRecognizeEnabled) {
            if (this::speechRecognizer.isInitialized && this::speechRecognizerIntent.isInitialized) {
                startSpeechRecognition()
            } else {
                initializeSpeechRecognition()
            }
            binding.downloading.text = ""
            binding.micButton?.setImageResource(R.drawable.mic)
        } else {
            if (this::speechRecognizer.isInitialized) {
                stopSpeechRecognition()
            }
            binding.maskDownloading.visibility = View.GONE
            binding.micButton?.setImageResource(R.drawable.mic_off)
        }
    }

    private fun startSpeechRecognition() {
        if (speechRecognizeEnabled) {
            speechRecognizer.startListening(speechRecognizerIntent)
            mStreamVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0; // getting system volume into var for later un-muting
            mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // setting system volume to zero, muting
            binding.micButton!!.visibility = View.VISIBLE
            binding.maskDownloading.visibility = View.VISIBLE
            binding.downloadingText.visibility = View.VISIBLE
            viewModel.downlaodingFirmwarePrev.value = true
            binding.downloadLottie.scaleX = 0.8F
            binding.downloadLottie.scaleY = 0.8F
            binding.downloadLottie.setAnimation(R.raw.microphone)
            binding.downloadLottie.repeatCount = LottieDrawable.INFINITE
            binding.downloadLottie.playAnimation()
            binding.downloadingText.text = getString(R.string.listening) //            binding.downloading.text = ""
        }
    }

    private fun stopSpeechRecognition() {
        z1 = 0;z2 = 0; z3 = 0; z4 = 0; z6 = 0; z7 = 0; z8 = 0; writeToFirebase()
        speechRecognizer.stopListening()
        binding.maskDownloading.visibility = View.GONE
    }

    private fun initializeSpeechRecognition() {
        if (!this::speechRecognizer.isInitialized) {
            mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?;
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0); // again setting the system volume back to the original, un-mutting
                }
                override fun onBeginningOfSpeech() {
                    binding.downloading.text = ""
                }
                override fun onRmsChanged(rmsdB: Float) {
                }
                override fun onBufferReceived(buffer: ByteArray?) {
                }
                override fun onEndOfSpeech() {
                    startSpeechRecognition()
                }
                @SuppressLint("SetTextI18n")
                override fun onError(error: Int) {
                    Log.d("SpeechMax", "onError $error")
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            startSpeechRecognition()
                            binding.downloading.text = "ERROR_NO_MATCH"
                        }
                        SpeechRecognizer.ERROR_AUDIO -> {
                            binding.downloading.text = "ERROR_AUDIO" //7
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            binding.downloading.text = "ERROR_CLIENT"
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            binding.downloading.text = "ERROR_RECOGNIZER_BUSY" //8
                        }
                        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> {
                            binding.downloading.text = "ERROR_TOO_MANY_REQUESTS"
                        }
                        else -> {
                            binding.downloading.text = "UNKNOWN ERROR"
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    try{
                    val result = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.joinToString(", ")
                    binding.downloading.text = result!!.removePrefix('['.toString()).removeSuffix(']'.toString())
                    Log.d("SpeechMax Results", result)
                    startSpeechRecognition()
                    excavatorAction(result)}
                    catch(_:java.lang.Exception){
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    Log.d("SpeechMax", "onPartialResults ${
                        partialResults!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).toString()
                    }")
                }
                override fun onEvent(eventType: Int, params: Bundle?) {
                }
            })
            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            //            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
//            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
//            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
        }
    }

    private fun excavatorAction(result: String) {
        when {
            result.contains("forward", ignoreCase = true) or result.contains("front", ignoreCase = true) or result.contains("ahead", ignoreCase = true) or result.contains("straight", ignoreCase = true) -> {
                z1 = -1; z8 = -1; writeToFirebase()
                if (!result.contains("infinite", ignoreCase = true) and !result.contains("non", ignoreCase = true)) Handler(Looper.getMainLooper()).postDelayed({ z1 = 0; z8 = 0; writeToFirebase() }, timerVoiceAction)
            }
            result.contains("reverse", ignoreCase = true) or result.contains("river", ignoreCase = true) or result.contains("back", ignoreCase = true) or result.contains("bak", ignoreCase = true) or result.contains("backward", ignoreCase = true) -> {
                z1 = 1; z8 = 1; writeToFirebase()
                if (!result.contains("infinite", ignoreCase = true) and !result.contains("non", ignoreCase = true)) Handler(Looper.getMainLooper()).postDelayed({ z1 = 0; z8 = 0; writeToFirebase() }, timerVoiceAction)
            }
            (result.contains("rotate", ignoreCase = true) or result.contains("turn", ignoreCase = true)) and (result.contains("right", ignoreCase = true) or result.contains("clockwise", ignoreCase = true)) -> {
                z2 = -1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z2 = 0; writeToFirebase() }, timerVoiceAction)
            }
            (result.contains("rotate", ignoreCase = true) or result.contains("turn", ignoreCase = true)) and (result.contains("left", ignoreCase = true) or result.contains("counter clockwise", ignoreCase = true) or result.contains("anti clockwise", ignoreCase = true)) -> {
                z2 = 1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z2 = 0; writeToFirebase() }, timerVoiceAction)
            }
            (result.contains("left", ignoreCase = true) or result.contains("anti clockwise", ignoreCase = true) or result.contains("counter clockwise", ignoreCase = true)) -> {
                z1 = 1; z8 = -1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z1 = 0; z8 = 0; writeToFirebase() }, timerVoiceAction)
            }
            (result.contains("right", ignoreCase = true) or result.contains("clockwise", ignoreCase = true)) -> {
                z1 = -1; z8 = 1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z1 = 0; z8 = 0; writeToFirebase() }, timerVoiceAction)
            }
            (result.contains("bucket", ignoreCase = true) or result.contains("angle", ignoreCase = true)) and (result.contains("raise", ignoreCase = true) or result.contains("race", ignoreCase = true) or result.contains("up", ignoreCase = true) or result.contains("lift", ignoreCase = true)) -> {
                z6 = -1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z6 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            (result.contains("bucket", ignoreCase = true) or result.contains("angle", ignoreCase = true)) and (result.contains("down", ignoreCase = true) or result.contains("lower", ignoreCase = true) or result.contains("descent", ignoreCase = true) or result.contains("town", ignoreCase = true)) -> {
                z6 = 1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z6 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            (result.contains("primary", ignoreCase = true) or result.contains("first", ignoreCase = true) or result.contains("arm", ignoreCase = true) or result.contains("main", ignoreCase = true) or result.contains("alpha", ignoreCase = true)) and (result.contains("down", ignoreCase = true) or result.contains("lower", ignoreCase = true) or result.contains("descent", ignoreCase = true) or result.contains("town", ignoreCase = true)) -> {
                z7 = -1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z7 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            (result.contains("primary", ignoreCase = true) or result.contains("first", ignoreCase = true) or result.contains("arm", ignoreCase = true) or result.contains("main", ignoreCase = true) or result.contains("alpha", ignoreCase = true)) and (result.contains("up", ignoreCase = true) or result.contains("lift", ignoreCase = true) or result.contains("raise", ignoreCase = true) or result.contains("race", ignoreCase = true)) -> {
                z7 = 1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z7 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            (result.contains("secondary", ignoreCase = true) or result.contains("second", ignoreCase = true)) and (result.contains("up", ignoreCase = true) or result.contains("lift", ignoreCase = true) or result.contains("raise", ignoreCase = true) or result.contains("race", ignoreCase = true)) -> {
                z3 = 1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z3 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            (result.contains("secondary", ignoreCase = true) or result.contains("second", ignoreCase = true)) and (result.contains("down", ignoreCase = true) or result.contains("lower", ignoreCase = true) or result.contains("descent", ignoreCase = true) or result.contains("town", ignoreCase = true)) -> {
                z3 = -1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z3 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            result.contains("engine", ignoreCase = true) or result.contains("sound", ignoreCase = true) -> {
                viewModel.engineSound()
            }
            result.contains("light", ignoreCase = true) or result.contains("lite", ignoreCase = true) -> {
                viewModel.engineLight()
            }
            result.contains("grab", ignoreCase = true) or result.contains("hold", ignoreCase = true) or result.contains("grip", ignoreCase = true) or result.contains("rap", ignoreCase = true) or result.contains("tap", ignoreCase = true) or result.contains("pick", ignoreCase = true) -> {
                z4 = 1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z4 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            result.contains("release", ignoreCase = true) or result.contains("leave", ignoreCase = true) or result.contains("drop", ignoreCase = true) -> {
                z4 = -1; writeToFirebase()
                Handler(Looper.getMainLooper()).postDelayed({ z4 = 0; writeToFirebase() }, timerVoiceActionLow)
            }
            result.contains("stop", ignoreCase = true) or result.contains("abort", ignoreCase = true) or result.contains("kill", ignoreCase = true) -> {
                z1 = 0;z2 = 0; z3 = 0; z4 = 0; z6 = 0; z7 = 0; z8 = 0; writeToFirebase()
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.RECORD_AUDIO)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH)
        }
        val missingPermissions = requiredPermissions.filter { permission ->
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            initializeBluetooth()
            requestCurrentLocation()
            initializeSpeechRecognition()
        } else {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), intBluetoothPermission)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            intBluetoothPermission -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) { // all permissions are granted
                    initializeBluetooth()
                    requestCurrentLocation()
                } else {
                    Toast.makeText(this, "Some or all bluetooth permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
            intLocationPermission -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "All location permission granted", Toast.LENGTH_SHORT).show()
                    requestCurrentLocation()
                } else {
                    Toast.makeText(this, "All location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            intCallPhoneRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Phone call permission granted", Toast.LENGTH_SHORT).show()
                    callExcavator(View(this))
                } else {
                    Toast.makeText(this, "Phone call permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            intSMSRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Send SMS permission granted", Toast.LENGTH_SHORT).show()
                    smsExcavator(View(this))
                } else {
                    Toast.makeText(this, "Send SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        val currentLocationTask = fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
        currentLocationTask.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                currentDeviceLocation = task.result
                val latLong = LatLng(currentDeviceLocation.latitude, currentDeviceLocation.longitude)
                markerDevice = googleMap.addMarker(MarkerOptions().position(latLong).title("Phone").visible(true))!!
                markerDevice.showInfoWindow()
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
                distanceToRobot()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth was turned ON", Toast.LENGTH_SHORT).show()
            Log.d("bluetooth adapter", "Bluetooth was turned ON")
            updateSerialData(serialText = "Broadcast Adapter : Bluetooth was turned ON\n", getColor(R.color.borderblueDark0gg))
            bluetoothAdapter.enable() //            bluetoothAdapter.getRemoteDevice("34:86:5D:5E:75:0E")
        }
        connectDevice(View(this))
    }

    @SuppressLint("CheckResult", "MissingPermission")
    fun connectDevice(view: View) {
        if (this::bluetoothManagerSerial.isInitialized) bluetoothManagerSerial.closeDevice(espBluetoothMacAddress)
        bluetoothManagerSerial = BluetoothManager.getInstance()
        val pairedDevices: Collection<BluetoothDevice> = bluetoothManagerSerial.pairedDevicesList
        for (device in pairedDevices) {
            Log.d("bluetoothManagerSerial", "Device name: " + device.name + " : " + device.address)
        }
        if (this::bluetoothManagerSerial.isInitialized) bluetoothManagerSerial.openSerialDevice(espBluetoothMacAddress).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::onConnected, this::onError)
    }

    private fun onConnected(connectedDevice: BluetoothSerialDevice) { // You are now connected to this device!
        deviceInterface = connectedDevice.toSimpleDeviceInterface()
        deviceInterface.setMessageReceivedListener(this::onMessageReceived)
        deviceInterface.setErrorListener(this::onError)
    }

    private fun onMessageReceived(message: String) { // We received a message! Handle it here.
        updateSerialData(serialText = message, getColor(R.color.teal_200))
        Log.d("bluetoothManagerSerial", "Received a message! Message was: $message")
    }

    private fun onError(error: Throwable) { // Handle the error
        //        Toast.makeText(this, "Error : ${error.localizedMessage}", Toast.LENGTH_LONG).show() // Replace context with your context instance.
        Log.d("bluetoothManagerSerial", "Error : ${error.localizedMessage}")
        updateSerialData(serialText = "BluetoothManagerSerialError : ${error.localizedMessage}\n", getColor(R.color.errorRed))
    }

    fun openCloseSerialWindow(view: View) {
        if (serialWindowOpen) {
            binding.serialWindow!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.zoomout_scoretable_close))
            Handler(Looper.getMainLooper()).postDelayed({ binding.serialWindow!!.visibility = View.GONE }, 240)
        } else {
            binding.serialWindow!!.visibility = View.VISIBLE
            binding.serialWindow!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.zoomin_scoretable_open))
        }
        serialWindowOpen = !serialWindowOpen
    }

    @SuppressLint("SetTextI18n")
    private fun usbAdapter1() {

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

    fun pickUpCall(view: View) {
        databaseRef.updateChildren(mapOf("AT" to "ATA"))
    }

    fun hangUpCall(view: View) {
        databaseRef.updateChildren(mapOf("AT" to "AT+cvhu=0"))
        Handler(Looper.getMainLooper()).postDelayed({ databaseRef.updateChildren(mapOf("AT" to "ATH")) }, 1000L)
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun receiveCallExcavator(view: View) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_NUMBERS), intReadPhoneNumber)
        } else {
            try {
                val telephoneManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val myPhoneNumber = telephoneManager.line1Number
                Toast.makeText(this, myPhoneNumber.substring(1), Toast.LENGTH_SHORT).show()
                databaseRef.updateChildren(mapOf("AT" to "ATD+44" + myPhoneNumber.substring(1) + ";"))
            } catch (_: Exception) {
                databaseRef.updateChildren(mapOf("AT" to "ATD+447496393966;"))
            }
        }
    }

    fun callExcavator(view: View) {
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

    fun smsExcavator(view: View) {
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

    private fun writeToFirebase() {
        databaseRef.updateChildren(mapOf(
            "z1" to zValue(z1), "z2" to zValue(z2),
            "z3" to zValue(z3), "z4" to zValue(z4), "z6" to zValue(z6),
            "z7" to zValue(z7), "z8" to zValue(z8),
        ))
    }

    fun startRobot(view: View) {
        startActivity(Intent(this, RobotActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        overridePendingTransition(R.anim.slide_left_activity, R.anim.slide_left_activity)
        finishAndRemoveTask()
    }

    private fun updateSerialData(serialText: String, color: Int = Color.WHITE) {
        serialTextArray.add(SerialText(serialText, color = color))
        serialTextAdapter.notifyItemInserted(serialTextArray.size - 1)
        binding.serialTextRecyclerView!!.scrollToPosition(serialTextArray.size - 1)
    }

    fun sendSerialCommand(view: View) {
        if (binding.editTextChatInput!!.text.toString().isNotEmpty()) {
            updateSerialData("Send : " + binding.editTextChatInput!!.text.toString(), Color.GRAY)
            if (this::usbSerialDevice.isInitialized) usbSerialDevice.write(binding.editTextChatInput!!.text.toString().toByteArray()) // send to USBSerial
            if (this::deviceInterface.isInitialized) deviceInterface.sendMessage((binding.editTextChatInput!!.text.toString() + "\n")) // send to bluetoothSerial
            binding.editTextChatInput!!.setText("")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSerialWindow(view: View) {
        serialTextArray.removeAll(serialTextArray.toSet())
        serialTextAdapter.notifyDataSetChanged()
    //        serialTextAdapter.notifyItemInserted(serialTextArray.size - 1)
        //        binding.serialTextRecyclerView!!.scrollToPosition(serialTextArray.size - 1)
    }

    private fun distanceToRobot() {
        if (this::currentDeviceLocation.isInitialized) {
//            val results = FloatArray(1)
//            val distance = Location.distanceBetween(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!, currentDeviceLocation.latitude, currentDeviceLocation.longitude, results)
//            Toast.makeText(this, "GPS distance ${results[0]}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeDataAndUpdateServer() {
        serialTextData.observe(this) {
            binding.serialText!!.text = it //            val key = serialDatabaseRef.child("R").push().key
            //            serialDatabaseRef.child("R").updateChildren(mapOf(key to it))

            serialTextDataTemp += it
            if (it.contains("\n")) {
                updateSerialData(serialTextDataTemp.replace("\n", ""))
                serialTextDataTemp = ""
            }

        }

        viewModel.downlaodingFirmware.observe(this) {
            when {
                it -> {
                    binding.maskDownloading.visibility = View.VISIBLE
                    binding.downloadingText.visibility = View.VISIBLE
                    binding.downloadingText.text = getString(R.string.please_wait)
                    viewModel.downlaodingFirmwarePrev.value = true
                    binding.downloadLottie.scaleX = 2.6F
                    binding.downloadLottie.scaleY = 2.6F
                    binding.downloadLottie.setAnimation(R.raw.downloading)
                    binding.downloadLottie.repeatCount = LottieDrawable.INFINITE
                    binding.downloadLottie.playAnimation()
                    binding.micButton!!.visibility = View.GONE
                    binding.downloading.text = getString(R.string.downloading_firmware_to_esp32)
                }
                viewModel.downlaodingFirmwarePrev.value == true -> {
                    viewModel.downlaodingFirmwarePrev.value = false
                    binding.downloadLottie.scaleX = 1.4F
                    binding.downloadLottie.scaleY = 1.4F
                    binding.downloadLottie.setAnimation(R.raw.done)
                    binding.downloadLottie.repeatCount = 0
                    binding.downloadLottie.playAnimation()
                    binding.micButton!!.visibility = View.GONE
                    binding.downloadingText.text = getString(R.string.please_wait)
                    binding.downloading.text = getString(R.string.updatefirmware)
                    binding.downloadingText.visibility = View.GONE
                    Handler(Looper.getMainLooper()).postDelayed({
                                                                    binding.micButton!!.visibility = View.VISIBLE
                                                                    binding.maskDownloading.visibility = View.GONE
                                                                }, 3000)
                }
                else -> {
                    binding.downloadingText.text = getString(R.string.please_wait)
                    viewModel.downlaodingFirmwarePrev.value = false
                    binding.maskDownloading.visibility = View.GONE
                    binding.micButton!!.visibility = View.VISIBLE
                }
            }
        }
        viewModel.gnssInfo.observe(this) {
            val latLong = LatLng(viewModel.gpsLatitude.value!!, viewModel.gpsLongitude.value!!)
            if (this::markerExcavator.isInitialized) markerExcavator.position = latLong
            if (this::googleMap.isInitialized) googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLong))
            distanceToRobot()
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
        binding.debugText.text = MotionEvent.actionToString(event!!.action) //+ "  " + v!!.id.toString()
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
                R.id.moveUp1 -> { // main arm
                    z7 = -1
                }
                R.id.moveDown1 -> { // main arm
                    z7 = 1
                }
                R.id.moveUp2 -> { //second arm 2
                    z3 = 1
                }
                R.id.moveDown2 -> { //second arm 2
                    z3 = -1
                }
                R.id.moveUp3 -> { //bucket angle
                    z6 = -1
                }
                R.id.moveDown3 -> { //bucket angle
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
            if (v.id == R.id.moveLeft1 || v.id == R.id.moveRight1) { //base rotate
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
        markerExcavator = googleMap.addMarker(MarkerOptions().position(latLong).title("Excavator").visible(true))!!
        markerExcavator.showInfoWindow()
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
        binding.mapViewExcavator.onResume()
    }

    override fun onStart() { //removed API key from here
        super.onStart()
        handler.post(updateFirebaseTask)
        binding.mapViewExcavator.onStart()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
        binding.mapViewExcavator.onStop()
    }

    override fun onPause() {
        binding.mapViewExcavator.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapViewExcavator.onDestroy()
        if (this::bluetoothManagerSerial.isInitialized) bluetoothManagerSerial.close();
        super.onDestroy()
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
    }
}


//private fun checkBluetoothPermissions() {
//    val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH)
//    } else {
//        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH)
//    }
//    val missingPermissions = requiredPermissions.filter { permission ->
//        checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
//    }
//    if (missingPermissions.isEmpty()) {
//        initializeBluetooth()
//    } else {
//        requestPermissions(missingPermissions.toTypedArray(), intBluetoothPermission)
//    }
//}
//
//private fun checkLocationPermission() {
//    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//        requestCurrentLocation()
//    } else {
//        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intLocationPermission)
//    }
//}