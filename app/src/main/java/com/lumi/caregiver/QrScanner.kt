package com.lumi.caregiver

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lumi.ui.BigButton
import java.util.concurrent.Executors

/**
 * Leitor de QR Code com a câmera traseira (CameraX + ZXing, sem Google Play Services).
 * Pede a permissão da câmera explicando o motivo; [onText] recebe cada QR lido, na thread principal.
 */
@Composable
fun QrScanner(onText: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by rememberSaveable { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        denied = !it
    }
    if (granted) {
        CameraQr(onText)
        return
    }
    Text("O LUMI usa a câmera só para ler o QR Code. Nenhuma foto é guardada.")
    if (denied) {
        Text(
            "Sem a câmera, você pode digitar o código. Para usar a câmera depois, libere a permissão nos ajustes do celular.",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    BigButton("Permitir câmera") { ask.launch(Manifest.permission.CAMERA) }
}

@Composable
private fun CameraQr(onText: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onText)
    val previewView = remember { PreviewView(context) }

    DisposableEffect(owner) {
        val main = ContextCompat.getMainExecutor(context)
        val worker = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            val p = future.get().also { provider = it }
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(worker) { image ->
                image.use { decodeQr(it.luminance(), it.width, it.height) }?.let { text -> main.execute { latest(text) } }
            }
            p.unbindAll()
            p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, main)
        onDispose {
            disposed = true
            provider?.unbindAll()
            worker.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .semantics { contentDescription = "Câmera ligada. Aponte para o QR Code." },
    )
}

/** Plano Y (tons de cinza) da imagem, sem o preenchimento de fim de linha. */
private fun ImageProxy.luminance(): ByteArray {
    val plane = planes[0]
    val buffer = plane.buffer
    val out = ByteArray(width * height)
    for (row in 0 until height) {
        buffer.position(row * plane.rowStride)
        buffer.get(out, row * width, width)
    }
    return out
}
