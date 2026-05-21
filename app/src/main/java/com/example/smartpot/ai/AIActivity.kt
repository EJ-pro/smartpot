package com.example.smartpot.ai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartpot.databinding.ActivityAiBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AIActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAiBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var emotionAnalyzer: EmotionAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        emotionAnalyzer = EmotionAnalyzer(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnAnalyze.setOnClickListener {
            takePhotoAndAnalyze()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndAnalyze() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()

                if (binding.rbEmotion.isChecked) {
                    analyzeEmotion(bitmap)
                } else {
                    analyzePlant(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
            }
        })
    }

    private fun analyzeEmotion(bitmap: Bitmap) {
        emotionAnalyzer.analyzeEmotion(bitmap, 
            onSuccess = { result ->
                binding.tvResult.text = "현재 감정: $result"
            },
            onFailure = { e ->
                Toast.makeText(this, "감정 인식 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun analyzePlant(bitmap: Bitmap) {
        // TFLite 모델이 없을 경우를 대비한 가이드 텍스트
        binding.tvResult.text = "식물 진단 모델을 로드 중이거나 설정이 필요합니다.\n(PlantDoctor.tflite 파일 필요)"
        
        // TODO: Implement TFLite Inference logic here
        // Example:
        // val diagnosis = plantDiagnosisManager.classify(bitmap)
        // binding.tvResult.text = "진단 결과: $diagnosis"
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "AIActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
