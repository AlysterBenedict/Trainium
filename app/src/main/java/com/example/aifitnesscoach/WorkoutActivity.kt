package com.example.aifitnesscoach

// --- REQUIRED IMPORTS ---
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// UPDATED: Import the new binding class
import com.example.aifitnesscoach.databinding.ActivityWorkoutBinding
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorkoutActivity : AppCompatActivity(), PoseLandmarkerHelper.ResultListener {

    // UPDATED: Use the new ActivityWorkoutBinding
    private lateinit var binding: ActivityWorkoutBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    // --- Workout State Variables ---
    private lateinit var workoutPlan: ArrayList<String>
    private var exerciseDuration: Long = 0
    private var restDuration: Long = 0
    private var currentExerciseIndex = 0
    private var countDownTimer: CountDownTimer? = null

    // --- Rep Counting State Variables (RESTORED) ---
    private lateinit var currentExerciseConfig: ExerciseConfig
    private var repCounter = 0
    private var exerciseStage = "" // e.g., "up" or "down"
    private var feedbackText = "Get Ready"
    private var jointColor = Color.GREEN

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val LEFT_HIP = 23; const val LEFT_KNEE = 25; const val LEFT_ANKLE = 27
        const val RIGHT_HIP = 24; const val RIGHT_KNEE = 26; const val RIGHT_ANKLE = 28
        const val LEFT_SHOULDER = 11; const val LEFT_ELBOW = 13; const val LEFT_WRIST = 15
        const val RIGHT_SHOULDER = 12; const val RIGHT_ELBOW = 14; const val RIGHT_WRIST = 16
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UPDATED: Inflate the new binding class
        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // --- Retrieve workout plan from Intent ---
        workoutPlan = intent.getStringArrayListExtra(Constants.EXTRA_WORKOUT_PLAN) ?: ArrayList()
        currentExerciseIndex = intent.getIntExtra(Constants.EXTRA_CURRENT_INDEX, 0)
        exerciseDuration = intent.getLongExtra(Constants.EXTRA_EXERCISE_DURATION, 30000)
        restDuration = intent.getLongExtra(Constants.EXTRA_REST_DURATION, 15000)

        if (currentExerciseIndex >= workoutPlan.size) {
            workoutComplete()
            return
        }

        val currentExerciseName = workoutPlan[currentExerciseIndex]
        currentExerciseConfig = Exercises.list.find { it.name == currentExerciseName }!!

        binding.exerciseTimerText.text = "${exerciseDuration / 1000}s"

        // --- Reset State for the new exercise ---
        resetExerciseState()
        updateUI()

        binding.skipExerciseButton.setOnClickListener { goToRest() }
        binding.viewFinder.post {
            if (allPermissionsGranted()) {
                setupPoseLandmarker()
                startCamera()
                startTimer(exerciseDuration)
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    private fun resetExerciseState() {
        repCounter = 0
        exerciseStage = if (currentExerciseConfig.invertStages) "down" else "up"
        feedbackText = "Ready"
    }

    private fun startTimer(duration: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                binding.exerciseTimerText.text = "${secondsLeft}s"
            }
            override fun onFinish() {
                goToRest()
            }
        }.start()
    }

    private fun goToRest() {
        countDownTimer?.cancel()
        val nextIndex = currentExerciseIndex + 1

        if (nextIndex >= workoutPlan.size) {
            workoutComplete()
        } else {
            val nextExerciseName = workoutPlan.getOrNull(nextIndex)
            val intent = Intent(this, RestActivity::class.java).apply {
                putExtra(Constants.EXTRA_REST_DURATION, restDuration)
                putExtra(Constants.EXTRA_NEXT_EXERCISE_NAME, nextExerciseName)
                putStringArrayListExtra(Constants.EXTRA_WORKOUT_PLAN, workoutPlan)
                putExtra(Constants.EXTRA_CURRENT_INDEX, nextIndex)
                putExtra(Constants.EXTRA_EXERCISE_DURATION, exerciseDuration)
            }
            startActivity(intent)
            finish()
        }
    }
    private fun workoutComplete() {
        startActivity(Intent(this, WorkoutCompleteActivity::class.java))
        finish()
    }

    private fun updateUI() {
        binding.exerciseNameText.text = currentExerciseConfig.name
        binding.repsCounterText.text = repCounter.toString()
        binding.feedbackText.text = feedbackText
    }

    private fun processExercise(poseLandmarks: List<NormalizedLandmark>) {
        currentExerciseConfig.postureValidation?.let { pvConfig ->
            val p1 = Landmark(poseLandmarks[pvConfig.landmarksToTrack[0]].x(), poseLandmarks[pvConfig.landmarksToTrack[0]].y())
            val p2 = Landmark(poseLandmarks[pvConfig.landmarksToTrack[1]].x(), poseLandmarks[pvConfig.landmarksToTrack[1]].y())
            val p3 = Landmark(poseLandmarks[pvConfig.landmarksToTrack[2]].x(), poseLandmarks[pvConfig.landmarksToTrack[2]].y())
            val postureAngle = calculateAngle(p1, p2, p3)

            if (postureAngle > pvConfig.angleThreshold) {
                feedbackText = pvConfig.feedbackIncorrect
                jointColor = Color.RED
                updateUI()
                return
            }
        }

        val landmarks = currentExerciseConfig.landmarksToTrack.map { Landmark(poseLandmarks[it].x(), poseLandmarks[it].y()) }
        val angle1 = calculateAngle(landmarks[0], landmarks[1], landmarks[2])
        val angle2 = calculateAngle(landmarks[3], landmarks[4], landmarks[5])

        val upThreshold = currentExerciseConfig.upThreshold!!
        val downThreshold = currentExerciseConfig.downThreshold!!
        val feedbackMap = currentExerciseConfig.feedbackMap

        if (!currentExerciseConfig.invertStages) {
            if (angle1 > upThreshold && angle2 > upThreshold) {
                exerciseStage = "up"
                feedbackText = feedbackMap["up"] ?: "Ready"
                jointColor = Color.rgb(245, 117, 66)
            } else if (angle1 < downThreshold && angle2 < downThreshold && exerciseStage == "up") {
                exerciseStage = "down"
                repCounter++
                feedbackText = feedbackMap["down"] ?: "Good Rep"
                jointColor = Color.GREEN
            } else {
                if(exerciseStage == "up") feedbackText = feedbackMap["transition_down"] ?: "Go Down"
                else feedbackText = feedbackMap["transition_up"] ?: "Go Up"
                jointColor = Color.RED
            }
        } else {
            if (angle1 < downThreshold && angle2 < downThreshold) {
                exerciseStage = "down"
                feedbackText = feedbackMap["down"] ?: "Ready"
                jointColor = Color.rgb(245, 117, 66)
            } else if (angle1 > upThreshold && angle2 > upThreshold && exerciseStage == "down") {
                exerciseStage = "up"
                repCounter++
                feedbackText = feedbackMap["up"] ?: "Good Rep"
                jointColor = Color.GREEN
            } else {
                if(exerciseStage == "down") feedbackText = feedbackMap["transition_up"] ?: "Go Up"
                else feedbackText = feedbackMap["transition_down"] ?: "Go Down"
                jointColor = Color.RED
            }
        }
        updateUI()
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            if (resultBundle.results.isEmpty() || resultBundle.results.first().landmarks().isEmpty()) {
                binding.overlay.clear()
                feedbackText = "No pose detected"
                updateUI()
                return@runOnUiThread
            }
            val poseLandmarks = resultBundle.results.first().landmarks().first()
            processExercise(poseLandmarks)
            binding.overlay.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM,
                jointColor
            )
        }
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        cameraExecutor.shutdown()
    }

    private fun setupPoseLandmarker() {
        poseLandmarkerHelper = PoseLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            poseLandmarkerHelperListener = this
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        poseLandmarkerHelper.detectLiveStream(image)
                    }
                }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e("WorkoutActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupPoseLandmarker()
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}