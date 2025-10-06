package com.example.aifitnesscoach

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.aifitnesscoach.databinding.ActivityStartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.Executor

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding
    private var glowPulseAnimator: ObjectAnimator? = null
    private var textPulseAnimator: ObjectAnimator? = null
    private var logoGlowAnimator: ObjectAnimator? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        animateLoadingBar()
    }

    private fun animateLoadingBar() {
        // Run all visual setup before starting animation
        displayRandomQuote()
        startGlowPulseAnimation()
        startAppNamePulseAnimation()
        startLogoGlowAnimation()

        val progressBar = binding.loadingProgressBar
        val progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        progressAnimator.duration = 1500
        progressAnimator.interpolator = DecelerateInterpolator()

        progressAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                // When animation ends, cancel others and check user status
                cancelAllAnimations()
                checkUserStatus()
            }
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        progressAnimator.start()
    }

    private fun checkUserStatus() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)

        if (auth.currentUser != null) {
            if (isBiometricEnabled) {
                showBiometricPrompt()
            } else {
                navigateToHome()
            }
        } else {
            navigateToLogin()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToHome()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication required to proceed.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Lock")
            .setSubtitle("Unlock Trainium to continue")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun cancelAllAnimations() {
        glowPulseAnimator?.cancel()
        textPulseAnimator?.cancel()
        logoGlowAnimator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllAnimations()
    }

    // --- ADDED BACK: Missing Animation and UI Functions ---

    private fun displayRandomQuote() {
        val quotes = resources.getStringArray(R.array.motivational_quotes)
        val randomQuote = quotes.random()
        binding.quoteTextView.text = "\"$randomQuote\""
    }

    private fun startGlowPulseAnimation() {
        val glowView = binding.glowView
        glowPulseAnimator = ObjectAnimator.ofFloat(glowView, "alpha", 0f, 0.7f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        glowPulseAnimator?.start()
    }

    private fun startAppNamePulseAnimation() {
        val appNameText = binding.appNameTextView
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.05f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.05f)
        textPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(appNameText, scaleX, scaleY).apply {
            duration = 1200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        textPulseAnimator?.start()
    }

    private fun startLogoGlowAnimation() {
        val logoGlow = binding.logoGlowView
        logoGlowAnimator = ObjectAnimator.ofFloat(logoGlow, "alpha", 0f, 0.8f).apply {
            duration = 1200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        logoGlowAnimator?.start()
    }
}