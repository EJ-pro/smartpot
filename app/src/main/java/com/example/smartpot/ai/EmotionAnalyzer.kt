package com.example.smartpot.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class EmotionAnalyzer(private val context: Context) {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    fun analyzeEmotion(bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onSuccess("표정을 읽을 수 없어요.")
                    return@addOnSuccessListener
                }

                val face = faces[0]
                val smileProb = face.smilingProbability ?: 0f
                
                val emotion = when {
                    smileProb > 0.7f -> "행복함"
                    smileProb > 0.3f -> "평온함"
                    else -> "슬픔/무표정"
                }
                onSuccess(emotion)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
