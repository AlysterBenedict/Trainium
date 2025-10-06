package com.example.aifitnesscoach

// NEW: Data class to define the structure for posture validation
data class PostureValidation(
    val landmarksToTrack: List<Int>,
    val angleThreshold: Double,
    val feedbackIncorrect: String
)

// Data class to define the structure of an exercise configuration
data class ExerciseConfig(
    val name: String,
    val landmarksToTrack: List<Int>,
    val upThreshold: Double? = null, // Nullable for timed exercises
    val downThreshold: Double? = null, // Nullable for timed exercises
    val feedbackMap: Map<String, String>,
    val invertStages: Boolean = false,
    val exerciseType: String = "rep_based", // "rep_based" or "timed"
    val correctFormAngle: Double? = null, // For timed exercises
    val postureValidation: PostureValidation? = null // NEW: For posture checks
)

// Object to hold all our defined exercises
object Exercises {
    val list = listOf(
        ExerciseConfig(
            name = "SQUAT",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_KNEE, WorkoutActivity.LEFT_ANKLE,
                WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_KNEE, WorkoutActivity.RIGHT_ANKLE
            ),
            upThreshold = 160.0,
            downThreshold = 120.0,
            feedbackMap = mapOf(
                "up" to "Ready", "down" to "Good Rep",
                "transition_up" to "Get Up Straight", "transition_down" to "Go Deeper"
            )
        ),
        ExerciseConfig(
            name = "BICEP CURL",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_ELBOW, WorkoutActivity.LEFT_WRIST,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_ELBOW, WorkoutActivity.RIGHT_WRIST
            ),
            upThreshold = 160.0,
            downThreshold = 40.0,
            feedbackMap = mapOf(
                "up" to "Ready", "down" to "Good Rep",
                "transition_up" to "Go Down Straight", "transition_down" to "Curl Up"
            )
        ),
        ExerciseConfig(
            name = "PUSH-UP",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_ELBOW, WorkoutActivity.LEFT_WRIST,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_ELBOW, WorkoutActivity.RIGHT_WRIST
            ),
            upThreshold = 160.0,
            downThreshold = 90.0,
            feedbackMap = mapOf(
                "up" to "Good Rep", "down" to "Ready",
                "transition_up" to "Push Up", "transition_down" to "Go Lower"
            ),
            invertStages = true
        ),
        ExerciseConfig(
            name = "LUNGE",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_KNEE, WorkoutActivity.LEFT_ANKLE,
                WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_KNEE, WorkoutActivity.RIGHT_ANKLE
            ),
            upThreshold = 160.0,
            downThreshold = 100.0,
            feedbackMap = mapOf(
                "up" to "Ready", "down" to "Good Rep",
                "transition_up" to "Push Back Up", "transition_down" to "Step Forward"
            )
        ),
        ExerciseConfig(
            name = "PLANK",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_ANKLE,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_ANKLE
            ),
            exerciseType = "timed",
            correctFormAngle = 160.0,
            feedbackMap = mapOf(
                "correct" to "Hold Position",
                "incorrect" to "Straighten Back"
            )
        ),
        ExerciseConfig(
            name = "OVERHEAD PRESS",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_ELBOW, WorkoutActivity.LEFT_WRIST,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_ELBOW, WorkoutActivity.RIGHT_WRIST
            ),
            upThreshold = 160.0,
            downThreshold = 90.0,
            feedbackMap = mapOf(
                "up" to "Good Rep", "down" to "Ready",
                "transition_up" to "Press Up", "transition_down" to "Lower Slowly"
            ),
            invertStages = true
        ),
        ExerciseConfig(
            name = "BENT OVER ROW",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_ELBOW, WorkoutActivity.LEFT_WRIST,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_ELBOW, WorkoutActivity.RIGHT_WRIST
            ),
            upThreshold = 160.0,
            downThreshold = 90.0,
            feedbackMap = mapOf(
                "up" to "Ready", "down" to "Good Squeeze",
                "transition_up" to "Lower Slowly", "transition_down" to "Pull!"
            ),
            postureValidation = PostureValidation(
                landmarksToTrack = listOf(WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_KNEE),
                angleThreshold = 110.0,
                feedbackIncorrect = "Bend Over More"
            )
        ),
        ExerciseConfig(
            name = "JUMPING JACKS",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_ELBOW,
                WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_ELBOW
            ),
            upThreshold = 140.0,
            downThreshold = 60.0,
            feedbackMap = mapOf(
                "up" to "Good Rep", "down" to "Ready",
                "transition_up" to "Arms Up!", "transition_down" to "Arms Down!"
            ),
            invertStages = true
        ),
        ExerciseConfig(
            name = "GLUTE BRIDGE",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_KNEE,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_KNEE
            ),
            upThreshold = 160.0,
            downThreshold = 100.0,
            feedbackMap = mapOf(
                "up" to "Good Squeeze", "down" to "Ready",
                "transition_up" to "Lift Hips", "transition_down" to "Lower Slowly"
            ),
            invertStages = true
        ),
        ExerciseConfig(
            name = "HIGH KNEES",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_KNEE,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_KNEE
            ),
            upThreshold = 150.0,
            downThreshold = 100.0,
            feedbackMap = mapOf(
                "up" to "Ready", "down" to "Good Rep",
                "transition_up" to "Knee Down", "transition_down" to "Knee Up!"
            )
        ),
        // NEW: Burpees configuration added
        ExerciseConfig(
            name = "BURPEES",
            landmarksToTrack = listOf(
                WorkoutActivity.LEFT_SHOULDER, WorkoutActivity.LEFT_HIP, WorkoutActivity.LEFT_KNEE,
                WorkoutActivity.RIGHT_SHOULDER, WorkoutActivity.RIGHT_HIP, WorkoutActivity.RIGHT_KNEE
            ),
            upThreshold = 160.0,
            downThreshold = 100.0,
            feedbackMap = mapOf(
                "up" to "Ready", "down" to "Good Rep",
                "transition_up" to "Jump Up", "transition_down" to "Chest to Floor"
            )
        )
    )
}

