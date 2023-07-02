package com.example.filepickerwebview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.FileProvider.*
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
            FilePickerWebViewTheme() {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WebViewWithFilePicker2(url)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CreditWebView(
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
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = 0
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
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

@Composable
fun WebViewWithFilePicker2(url: String) {
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    var timeViewState by remember {
        mutableStateOf(false)
    }

    if (timeViewState) {
        TwoOptionsDialog(filePathCallback,timeViewState)
    }

    val webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            callback: ValueCallback<Array<Uri>>?,
            params: FileChooserParams?
        ): Boolean {
            filePathCallback = callback
            timeViewState = true
            return true
        }
    }

    CreditWebView(webViewClient = WebViewClient(), webChromeClient = webChromeClient, url = url)
}

private fun openFilePicker(filePickerLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE)
    }
    filePickerLauncher.launch(intent)
}

private fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}

private fun CheckPermissionAndOpenCamera(
    context: Context,
    uri: Uri,
    cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
) {
    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
        cameraLauncher.launch(uri)
    } else {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}


@Composable
fun TwoOptionsDialog(filePathCallback: ValueCallback<Array<Uri>>?, timeViewState: Boolean) {
    val showDialog = rememberSaveable { mutableStateOf(true) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val data = result.data
        val uri: Uri? = data?.data
        val selectedFiles = uri?.let { arrayOf(it) }
        filePathCallback?.onReceiveValue(selectedFiles)
    }

    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider", file
    )

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        val selectedFiles = uri?.let { arrayOf(it) }
        filePathCallback?.onReceiveValue(selectedFiles)
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

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                Text(
                    text = "Option Chooser",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Choose what to open ?",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        openFilePicker(filePickerLauncher)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Open File Picker")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        CheckPermissionAndOpenCamera(
                            context = context,
                            uri = uri,
                            cameraLauncher = cameraLauncher,
                            permissionLauncher = permissionLauncher
                        )
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Open Camera")
                }
            }
        )
    }
}