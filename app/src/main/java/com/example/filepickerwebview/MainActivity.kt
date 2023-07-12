package com.example.filepickerwebview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.annotation.ExperimentalCoilApi
import com.example.filepickerwebview.ui.theme.FilePickerWebViewTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val url = "https://beautrix.co.uk/filepicker.html"
            FilePickerWebViewTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WebViewWithFilePicker(url)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    webViewClient: WebViewClient,
    webChromeClient: WebChromeClient,
    url: String
) {
    handleCookies()
    AndroidView(factory = {
        WebView(it).apply {
            this.webViewClient = webViewClient
            this.webChromeClient = webChromeClient
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
            }
            loadUrl(url)
        }
    }, modifier = Modifier.fillMaxWidth())
}

private fun handleCookies() {
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
        setAcceptCookie(true)
        acceptCookie()
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun WebViewWithFilePicker(url: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(color = Color.Blue)
        ) {
            WebView(webViewClient = WebViewClient(), webChromeClient = getWebChromeClient(), url = url)
        }

    }
}

@Composable
private fun getWebChromeClient(): WebChromeClient {
    var capturedImageUri by remember { mutableStateOf(Uri.EMPTY) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var shouldShowFilePickerDialogState by remember { mutableStateOf(false) }
    val onFilesSelected: OnFilesSelectedCallback = { selectedFiles ->
        selectedFiles?.forEach { Log.d("aamir-files", it.toString()) }
        filePathCallback?.onReceiveValue(selectedFiles)
        selectedFiles?.firstOrNull()?.let {
            capturedImageUri = it
        }
        filePathCallback = null
        shouldShowFilePickerDialogState = false
    }

    if (shouldShowFilePickerDialogState) {
        FilePickerDialog(callback = onFilesSelected, onDismissRequest = {
            shouldShowFilePickerDialogState = false
            filePathCallback?.onReceiveValue(null)
        })
    }

    val webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            callback: ValueCallback<Array<Uri>>?,
            params: FileChooserParams?
        ): Boolean {
            filePathCallback = callback
            shouldShowFilePickerDialogState = true
            return true
        }
    }
    return webChromeClient
}

private fun openFilePicker(filePickerLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE)
    }
    filePickerLauncher.launch(intent)
}

private fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val image = File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir
    )
    return image
}

private fun checkPermissionAndOpenCamera(
    context: Context,
    uri: Uri,
    cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
        cameraLauncher.launch(uri)
    } else {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

@Composable
fun FilePickerDialog(callback: OnFilesSelectedCallback, onDismissRequest: () -> Unit) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val data = result.data
        val uri: Uri? = data?.data
        val selectedFiles = uri?.let { arrayOf(it) }
        callback(selectedFiles)
    }

    val context = LocalContext.current
    var file by remember { mutableStateOf<File>(context.createImageFile()) }

    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context), "${BuildConfig.APPLICATION_ID}.provider", file
    )

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        callback(arrayOf(uri))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
    Dialog(
        onDismissRequest = { onDismissRequest.invoke() },
        content = {
            Column(
                Modifier
                    .background(Color.White)
                    .padding(16.dp)) {
                Text(text = "Choose an option")
                Button(
                    onClick = { openFilePicker(filePickerLauncher) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(text = "Choose File") }
                Button(
                    onClick = {
                        checkPermissionAndOpenCamera(
                            context = context,
                            uri = uri,
                            cameraLauncher = cameraLauncher,
                            permissionLauncher = permissionLauncher
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Take Photo")
                }
            }
        },
    )
}

typealias OnFilesSelectedCallback = (Array<Uri>?) -> Unit
