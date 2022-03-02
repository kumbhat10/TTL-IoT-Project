package com.ttl.robotcontrol

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewbinding.BuildConfig
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.ttl.robotcontrol.databinding.ActivitySplashBinding
import java.text.SimpleDateFormat
import java.util.*


class Splash : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var clickSound: MediaPlayer
    private lateinit var backgroundMusic: MediaPlayer
    private val timerLoading = 1000L
    private var state = 0
    private var machine = ""
    private var allowStart = false
    private var fastStart = false
    private lateinit var loadingAnim: ObjectAnimator
    private lateinit var fireStoreRef: DocumentReference

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

        binding = ActivitySplashBinding.inflate(layoutInflater)
        FirebaseApp.initializeApp(this);

        setContentView(binding.root)
        backgroundMusic = MediaPlayer.create(this, R.raw.inspiring)
//        backgroundMusic.setVolume(0.07F, 0.07F)
        backgroundMusic.start()

        if (BuildConfig.DEBUG) {
            allowStart = true
        }
        updateUI()

        clickSound = MediaPlayer.create(this, R.raw.success)
        clickSound.setVolume(0.06F, 0.06F)

        fireStoreRef =
            Firebase.firestore.collection("Users").document(FirebaseAuth.getInstance().uid.toString())
    }


    private fun updateUI() {

        val avd = binding.ttlLogo.drawable as AnimatedVectorDrawable
        avd.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                clickSound.start()
                binding.motionLogo.setTransition(R.id.start, R.id.end)
                binding.motionLogo.transitionToEnd()
                getToken()
                subscribeTopic()
            }
        })
        avd.start()

        loadingAnim = ObjectAnimator.ofInt(binding.loading, "progress", 0, 10000)
        loadingAnim.duration = timerLoading
        loadingAnim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                when (state) {
                    0 -> {
                        state = 1
                        binding.warmingup.text = getString(R.string.serverConnect)
                        loadingAnim.duration = 1200L
                        binding.lottieView.scaleX = 0.9F
                        binding.lottieView.scaleY = 0.9F
                        binding.wait.visibility = View.GONE
                        binding.lottieView.setAnimation(R.raw.cloudsync)
                        binding.lottieView.playAnimation()
                        loadingAnim.start()
                    }
                    1 -> {
                        state = 2
                        loadingAnim.duration = 1200L
                        binding.lottieView.scaleX = 1.1F
                        binding.lottieView.scaleY = 1.1F
                        binding.wait.visibility = View.GONE
                        if (machine == "RobotArm") {
                            binding.warmingup.text = getString(R.string.wakingrobot)
                            binding.lottieView.setAnimation(R.raw.robot)
                        }
                        if (machine == "Excavator") {
                            binding.warmingup.text = getString(R.string.wakingrobot)
                            binding.lottieView.setAnimation(R.raw.excavator)
                        }
                        binding.lottieView.playAnimation()
                        resetPosition()
                        loadingAnim.start()
                    }
                    2 -> {
                        state = 3
                        binding.warmingup.text = getString(R.string.connected)
                        binding.wait.visibility = View.GONE
                        binding.lottieView.scaleX = 1.4F
                        binding.lottieView.scaleY = 1.4F
                        binding.lottieView.setAnimation(R.raw.done)
                        binding.lottieView.repeatCount = 0
                        binding.lottieView.playAnimation()
                        binding.loading.visibility = View.GONE
                        Handler(Looper.getMainLooper()).postDelayed({
//                            binding.warmingup.text = getString(R.string.startClick)
                            allowStart = true
                            if (machine == "RobotArm") startRobotActivity(View(this@Splash))
                            if (machine == "Excavator") startExcavatorActivity(View(this@Splash))
                        }, 600)
                    }
                }
            }
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
        })
    }

    fun selectClientMachine(view: View) {
        machine = view.tag.toString()
        if (machine == "RobotArm" && BuildConfig.DEBUG) startRobotActivity(View(this@Splash))
        else if (machine == "Excavator" && BuildConfig.DEBUG) startExcavatorActivity(View(this@Splash))
        else {
            binding.motionLogo.setTransitionListener(object : MotionLayout.TransitionListener {
                override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {}
                override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {}
                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    binding.motionLogo.visibility = View.GONE
                    binding.lottieView.visibility = View.VISIBLE
                    binding.loading.visibility = View.VISIBLE
                    binding.warmingup.visibility = View.VISIBLE
                    binding.wait.visibility = View.VISIBLE
                    loadingAnim.start()
                }

                override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}
            })
            binding.motionLogo.setTransition(R.id.endToFinish)
            binding.motionLogo.transitionToEnd()
        }
    }

    private fun subscribeTopic() {
        Firebase.messaging.subscribeToTopic("Alert")
            .addOnCompleteListener { task ->
                var msg = "Cloud notification subscribed"
                if (!task.isSuccessful) {
                    msg = "Cloud notification subscription Failed"
                }
//                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result            // Get new FCM registration token
//            Firebase.database.getReference("FCM_Token").setValue(token)
            fireStoreRef.set(hashMapOf("LSDT" to SimpleDateFormat("HH:mm:ss z").format(Date()), "lang" to applicationContext.resources.configuration.locale.displayLanguage, "VC" to packageManager.getPackageInfo(packageName, 0).versionName.toString()), SetOptions.merge())                    //					checkAccessToTrain()

        })
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
    }

    override fun onPause() {
        backgroundMusic.pause()
        super.onPause()
    }

    override fun onStart() {
        backgroundMusic.start()
        super.onStart()
    }

    override fun onStop() {
        loadingAnim.removeAllListeners()
        loadingAnim.removeAllUpdateListeners()
        loadingAnim.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        clickSound.release()
        backgroundMusic.release()
        super.onDestroy()
    }

    fun startRobotActivity(view: View) {
        if (allowStart) startActivity(Intent(this, RobotActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        overridePendingTransition(R.anim.slide_left_activity, R.anim.slide_left_activity)
        finishAndRemoveTask()
    }

    fun startExcavatorActivity(view: View) {
        if (allowStart) startActivity(Intent(this, ExcavatorActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        overridePendingTransition(R.anim.slide_left_activity, R.anim.slide_left_activity)
        finishAndRemoveTask()
    }

}