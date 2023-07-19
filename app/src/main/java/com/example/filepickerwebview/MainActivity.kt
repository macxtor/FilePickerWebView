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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.annotation.ExperimentalCoilApi
import com.example.filepickerwebview.ui.theme.FilePickerWebViewTheme
import kotlinx.coroutines.launch
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
                    Home(url = url)
                }
            }
        }
    }
}
@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WebViewWithFilePicker(url: String, showBottomSheet: (ValueCallback<Array<Uri>>?) -> Unit) {
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
            WebView(
                webViewClient = WebViewClient(),
                webChromeClient = getWebChromeClient(showBottomSheet = showBottomSheet),
                url = url
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun getWebChromeClient(showBottomSheet: (ValueCallback<Array<Uri>>?) -> Unit) = object : WebChromeClient() {
    override fun onShowFileChooser(
        webView: WebView?,
        callback: ValueCallback<Array<Uri>>?,
        params: FileChooserParams?
    ): Boolean {
        showBottomSheet(callback)
        return true
    }
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

typealias OnFilesSelectedCallback = (Array<Uri>?) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(url: String) {
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = SheetState(
            skipPartiallyExpanded = false,
            initialValue = SheetValue.Hidden,
            confirmValueChange = { it != SheetValue.PartiallyExpanded }
        )
    )
    val closeBottomSheet = {
        coroutineScope.launch { sheetState.bottomSheetState.hide() }
    }
    var capturedImageUri by remember { mutableStateOf(Uri.EMPTY) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val onFilesSelected: OnFilesSelectedCallback = { selectedFiles ->
        selectedFiles?.forEach { Log.d("aamir-files", it.toString()) }
        filePathCallback?.onReceiveValue(selectedFiles)
        selectedFiles?.firstOrNull()?.let {
            capturedImageUri = it
        }
        filePathCallback = null
        closeBottomSheet()
    }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetShape = RoundedCornerShape(20.dp),
        sheetPeekHeight = 200.dp,
        sheetContent = {
            MyBottomSheet(
                onClose = {
                    filePathCallback?.onReceiveValue(null)
                    coroutineScope.launch { sheetState.bottomSheetState.hide() }
                },
                callback = onFilesSelected
            )
        },
        content = {
            WebViewWithFilePicker(
                url = url,
                showBottomSheet = { valueCallback ->
                    filePathCallback = valueCallback
                    coroutineScope.launch { sheetState.bottomSheetState.expand() }
                },
            )
        }
    )
}

@Composable
fun MyBottomSheet(callback: OnFilesSelectedCallback, onClose: () -> Unit) {
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
    Column(
        modifier = Modifier
            .padding(32.dp)
            .height(250.dp)
    ) {
        Text(
            text = "Bottom sheet",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(text = "Choose an option", style = MaterialTheme.typography.bodyLarge)
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
        Button(
            onClick = { openFilePicker(filePickerLauncher) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(text = "Choose File") }
    }
}