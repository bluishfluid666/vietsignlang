 package com.example.vietsignlang
import VSLWebsocketClient
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.vietsignlang.ui.theme.VietSignLangTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread


 class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bitmapBuffer: Bitmap
    private var webSocketClient = VSLWebsocketClient()
    private val senderChannel = Channel<Bitmap>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val receiverChannel = Channel<String>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        OpenCVLoader.initLocal()
        setContent {
            VietSignLangTheme {
                val scaffoldState = rememberBottomSheetScaffoldState()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val previewView = remember { PreviewView(context) }
                var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
                var translationString by remember {
                    mutableStateOf("Unknown")
                }

                LaunchedEffect(cameraSelector) {

                    launch {
                        webSocketClient.sendAndReceiveResult(senderChannel, receiverChannel)
                    }

//                    launch {
//                        webSocketClient.receiveInference(receiverChannel)
//                    }


                    launch {
                        setupCamera(previewView, lifecycleOwner, cameraSelector)
                    }

                    launch {
                        for (result in receiverChannel) {
                            translationString = if (result != "Unknown") result else translationString
                        }
                    }
                }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {}
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(alignment = Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(onClick = {
                                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Switch camera")
                            }
                        }
                        TranslationText(translation = translationString)
                    }
                }
            }
        }
    }

    private fun setupCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner, cameraSelector: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }


            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { image ->

                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running

                    bitmapBuffer = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888
                    )
                }
//                 Copy out RGB bits to our shared buffer
                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                val srcMat = Mat()
                Utils.bitmapToMat(bitmapBuffer, srcMat)
                val dstMat = Mat()
                Imgproc.resize(srcMat, dstMat, Size(224.0,224.0))
                val resizedBitmapBuffer = Bitmap.createBitmap(
                    224, 224, Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(dstMat, resizedBitmapBuffer)
                // TODO:
                runBlocking {
//                    webSocketClient.sendFrame(senderChannel)
                    senderChannel.send(resizedBitmapBuffer)
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(previewView.context))
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.disconnect()
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }
}

