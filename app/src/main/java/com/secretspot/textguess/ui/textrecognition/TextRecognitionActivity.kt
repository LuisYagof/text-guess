package com.secretspot.textguess.ui.textrecognition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.secretspot.textguess.databinding.ActivityTextRecognitionBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TextRecognitionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTextRecognitionBinding
    private val viewModel: TextRecognitionViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextRecognitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.recognizedText.observe(this) { text ->
            binding.recognizedTextView.text = text
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            binding.captureButton.isEnabled = !isProcessing
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        viewModel.setProcessing(true)

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImage(image)
                }

                override fun onError(exc: ImageCaptureException) {
                    viewModel.setProcessing(false)
                    Toast.makeText(
                        this@TextRecognitionActivity,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image).addOnSuccessListener { visionText ->
                    val processedText = processRecognizedText(visionText.text)
                    viewModel.updateRecognizedText(processedText)
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                }.addOnCompleteListener {
                    viewModel.setProcessing(false)
                    imageProxy.close()
                }
        }
    }

    private fun processRecognizedText(text: String): String {
        return text.replace(
            """(?<=\d)°(?=[CF])""".toRegex(),
            "º"
        )  // Replace ° with º after numbers and before C/F
            .replace("0C", "ºC").replace("0F", "ºF").replace("oC", "ºC").replace("oF", "ºF")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture =
                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    this, "Failed to start camera: ${exc.message}", Toast.LENGTH_SHORT
                ).show()
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
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}