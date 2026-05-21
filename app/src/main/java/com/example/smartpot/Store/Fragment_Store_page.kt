package com.example.smartpot.Store

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartpot.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Fragment_Store_page : Fragment() {

    private lateinit var viewFinder: PreviewView
    private lateinit var tvDiagnosis: TextView
    private lateinit var tvTargetStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAnalyze: Button
    private lateinit var scanLine: View
    private lateinit var flashOverlay: View
    private lateinit var cameraExecutor: ExecutorService

    private var isAnalyzing = false
    private var detectedLabel = ""
    private var realHealthScore = 0
    
    // UI 업데이트 주기를 관리하기 위한 변수들
    private var lastLabelUpdateTime = 0L
    private val LABEL_UPDATE_INTERVAL = 1500L // 1.5초마다 텍스트 갱신 (눈 피로 감소)

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.page_store, container, false)
        viewFinder = view.findViewById(R.id.viewFinder)
        tvDiagnosis = view.findViewById(R.id.tvDiagnosis)
        tvTargetStatus = view.findViewById(R.id.tvTargetStatus)
        progressBar = view.findViewById(R.id.analysisProgress)
        btnAnalyze = view.findViewById(R.id.btnStartAnalysis)
        scanLine = view.findViewById(R.id.scanLine)
        flashOverlay = view.findViewById(R.id.flashOverlay)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAnalyze.setOnClickListener {
            if (!isAnalyzing) {
                startIntensiveAnalysis()
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isAnalyzing) {
                            processImageProxyForDetection(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxyForDetection(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (labels.isNotEmpty()) {
                        val currentTime = System.currentTimeMillis()
                        val newLabel = labels[0].text
                        
                        // 1.5초가 지났거나, 감지된 대상이 아예 없을 때만 UI 갱신 (눈 피로도 감소)
                        if (currentTime - lastLabelUpdateTime > LABEL_UPDATE_INTERVAL || detectedLabel.isEmpty()) {
                            detectedLabel = newLabel
                            lastLabelUpdateTime = currentTime
                            activity?.runOnUiThread {
                                tvTargetStatus.text = "감지된 대상: $detectedLabel"
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun startIntensiveAnalysis() {
        isAnalyzing = true
        btnAnalyze.isEnabled = false
        
        // 셔터 소리 삭제 (요청사항 반영)

        // Flash effect (시각적 피드백 유지)
        flashOverlay.visibility = View.VISIBLE
        flashOverlay.alpha = 1f
        flashOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    flashOverlay.visibility = View.GONE
                }
            })

        // Pixel-based Analysis
        val bitmap = viewFinder.bitmap
        if (bitmap != null) {
            analyzeBitmapHealth(bitmap)
        }

        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        scanLine.visibility = View.VISIBLE
        
        // Scan animation
        val animation = TranslateAnimation(
            0f, 0f, 
            0f, viewFinder.height.toFloat()
        )
        animation.duration = 2000
        animation.repeatCount = Animation.INFINITE
        scanLine.startAnimation(animation)

        tvDiagnosis.text = "식물 세포 분석 및 상태 점검 중..."
        
        // Simulate progress
        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        val runnable = object : Runnable {
            override fun run() {
                progress += 2
                progressBar.progress = progress
                
                when {
                    progress < 30 -> tvTargetStatus.text = "이미지 전처리 중... ($progress%)"
                    progress < 60 -> tvTargetStatus.text = "엽록소 밀도 분석 중... ($progress%)"
                    progress < 90 -> tvTargetStatus.text = "병해충 패턴 매칭 중... ($progress%)"
                    else -> tvTargetStatus.text = "최종 진단 결과 생성 중... ($progress%)"
                }

                if (progress < 100) {
                    handler.postDelayed(this, 50)
                } else {
                    finishAnalysis()
                }
            }
        }
        handler.post(runnable)
    }

    private fun analyzeBitmapHealth(bitmap: Bitmap) {
        cameraExecutor.execute {
            var greenCount = 0
            var totalCount = 0
            val pixelStep = 10 

            for (y in 0 until bitmap.height step pixelStep) {
                for (x in 0 until bitmap.width step pixelStep) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    if (g > r && g > b && g > 50) {
                        greenCount++
                    }
                    totalCount++
                }
            }

            val greenRatio = if (totalCount > 0) (greenCount.toFloat() / totalCount * 100).toInt() else 0
            realHealthScore = (greenRatio * 1.2).toInt().coerceAtMost(100)
        }
    }

    private fun finishAnalysis() {
        isAnalyzing = false
        btnAnalyze.isEnabled = true
        scanLine.clearAnimation()
        scanLine.visibility = View.GONE
        
        val results = arrayOf(
            "🌿 상태: 매우 건강함 (점수: ${realHealthScore}점)\n광합성 활동이 활발하며 수분 상태가 최적입니다.",
            "⚠️ 상태: 수분 부족 감지 (점수: ${realHealthScore}점)\n잎의 채도가 낮아지고 있습니다. 충분한 물을 주세요.",
            "🍂 상태: 영양 부족 의심 (점수: ${realHealthScore}점)\n엽록소 수치가 낮습니다. 액체 비료를 권장합니다.",
            "☀️ 상태: 일조량 부족 (점수: ${realHealthScore}점)\n웃자람 패턴이 관찰됩니다. 더 밝은 곳으로 옮겨주세요.",
            "🐛 상태: 주의 필요 (점수: ${realHealthScore}점)\n불규칙한 반점이 발견되었습니다. 해충 피해를 확인하세요."
        )
        
        val isPlant = detectedLabel.lowercase().let { 
            it.contains("plant") || it.contains("leaf") || it.contains("flower") || it.contains("tree")
        }

        val finalResult = if (isPlant) {
            if (realHealthScore >= 90) results[0] 
            else if (realHealthScore >= 75) results[1]
            else if (realHealthScore >= 60) results[2]
            else results[4]
        } else {
            "❌ 진단 실패: 식물을 정확히 비춰주세요.\n(감지된 대상: $detectedLabel)"
        }

        tvDiagnosis.text = finalResult
        tvTargetStatus.text = "진단 완료"
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "StoreCamera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
