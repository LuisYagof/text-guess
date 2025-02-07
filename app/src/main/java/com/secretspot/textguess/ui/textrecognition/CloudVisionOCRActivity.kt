package com.secretspot.textguess.ui.textrecognition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Feature.Type
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageAnnotatorSettings
import com.google.protobuf.ByteString
import com.secretspot.textguess.R
import com.secretspot.textguess.databinding.ActivityTextRecognitionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CloudVisionOCRActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTextRecognitionBinding
    private val viewModel: TextRecognitionViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var visionClient: ImageAnnotatorClient
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextRecognitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeVisionClient()

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

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImage(image)
                }

                override fun onError(exc: ImageCaptureException) {
                    viewModel.setProcessing(false)
                    Toast.makeText(
                        this@CloudVisionOCRActivity,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun initializeVisionClient() {
        try {
            val credentials =
                GoogleCredentials.fromStream(resources.openRawResource(R.raw.credentials))
            visionClient = ImageAnnotatorClient.create(
                ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()
            )
        } catch (e: IOException) {
            Log.e("Vision", "Error initializing client", e)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            viewModel.setProcessing(true)

            try {
                // Convert ImageProxy to bytes
                val bytes = ImageUtil.imageProxyToByteArray(imageProxy)

                // Create Vision API image
                val image = com.google.cloud.vision.v1.Image.newBuilder()
                    .setContent(ByteString.copyFrom(bytes)).build()

                // Create request
                val feature = Feature.newBuilder().setType(Type.TEXT_DETECTION).build()

                val request =
                    AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build()

                // Call Vision API
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = visionClient.batchAnnotateImages(listOf(request))
                        val texts =
                            response.responsesList.firstOrNull()?.textAnnotationsList?.firstOrNull()?.description
                                ?: "No text found"

                        withContext(Dispatchers.Main) {
                            viewModel.updateRecognizedText(texts)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            println("ERROR VISION API CALL: " + e.message)
                            Toast.makeText(
                                this@CloudVisionOCRActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            viewModel.setProcessing(false)
                        }
                        imageProxy.close()
                    }
                }
            } catch (e: Exception) {
                Log.e("Vision", "Error processing image", e)
                imageProxy.close()
            }
        }
    }

    // Helper class for image conversion
    object ImageUtil {
        fun imageProxyToByteArray(image: ImageProxy): ByteArray {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return bytes
        }
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