package com.ttl.robotcontrol

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.datatransport.BuildConfig
import com.ttl.robotcontrol.databinding.ActivitySplashBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging


class Splash : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var clickSound: MediaPlayer
    private val timerLoading = 3000L
    private var state = 0
    private var allowStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(BuildConfig.DEBUG) allowStart = true
        updateUI()
        getToken()
        subscribeTopic()
        clickSound = MediaPlayer.create(this, R.raw.done_sound)
    }

    override fun onDestroy() {
        clickSound.release()
        super.onDestroy()
    }
    private fun fakeCrashSetup(){
        val crashButton = Button(this)
        crashButton.text = "Test Crash"
        crashButton.setOnClickListener {
            throw RuntimeException("Test Crash") // Force a crash
        }
        addContentView(crashButton, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    private fun updateUI(){
//        fakeCrashSetup()
        val loadingAnim = ObjectAnimator.ofInt(binding.loading, "progress", 0, 10000)
        loadingAnim.duration = timerLoading
        loadingAnim.addListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                when(state){
                    0->{
                        state = 1
//                        clickSound.start()
                        binding.warmingup.text = getString(R.string.serverConnect)
                        binding.lottieView.scaleX = 1F
                        binding.lottieView.scaleY = 1F
                        binding.wait.visibility = View.GONE
                        binding.lottieView.setAnimation(R.raw.cloudsync)
                        binding.lottieView.playAnimation()
                        loadingAnim.duration = 3000L
                        loadingAnim.start()
                    }
                    1->{
                        state = 2
                        clickSound.start()
                        binding.warmingup.text = getString(R.string.wakingrobot)
                        loadingAnim.duration = 4000L
                        binding.wait.visibility = View.GONE
                        binding.lottieView.setAnimation(R.raw.robot)
                        binding.lottieView.playAnimation()
                        loadingAnim.start()
                    }
                    2->{
                        state = 3
                        clickSound.start()
                        binding.warmingup.text = getString(R.string.connected)
                        binding.wait.visibility = View.GONE
                        binding.lottieView.scaleX = 1.4F
                        binding.lottieView.scaleY = 1.4F
                        binding.lottieView.setAnimation(R.raw.done)
                        binding.lottieView.repeatCount = 0
                        binding.lottieView.playAnimation()
                        binding.loading.visibility = View.GONE
                        Firebase.database.getReference("Control/data").setValue(FirebaseData())
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.warmingup.text = getString(R.string.startClick)
                            allowStart = true
                        }, 1500)
                    }
                }
            }
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
        })

        val ttlAnimation = AnimationUtils.loadAnimation(this, R.anim.ttl_logo_intro)
        ttlAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                binding.lottieView.visibility = View.VISIBLE
                binding.loading.visibility = View.VISIBLE
                binding.warmingup.visibility = View.VISIBLE
                binding.wait.visibility = View.VISIBLE
                loadingAnim.start()
            }
            override fun onAnimationRepeat(p0: Animation?) {}
        } )
        ttlAnimation.fillAfter = true
        binding.ttlLogo.startAnimation(ttlAnimation)
    }
    private fun subscribeTopic() {
        Firebase.messaging.subscribeToTopic("Alert")
            .addOnCompleteListener { task ->
                var msg = "Subscription successful"
                if (!task.isSuccessful) {
                    msg = "Subscription Failed"
                }
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun getToken(){
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result            // Get new FCM registration token
            Firebase.database.getReference("FCM_Token").setValue(token)
            Toast.makeText(baseContext, "Token registered to cloud", Toast.LENGTH_SHORT).show()
        })
    }
    fun startNextActivity(view: View){
        if (allowStart) startActivity(Intent(this, FullscreenActivity::class.java))
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
    }
}