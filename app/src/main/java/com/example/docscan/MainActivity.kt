package com.example.docscan

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.docscan.ui.theme.DocScanTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import kotlin.text.insert

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        // now we will instantiate a instance scanner  in which we will pass the options
        val scanner = GmsDocumentScanning.getClient(options)

        enableEdgeToEdge()
        setContent {
            DocScanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
//                            result ->
//                        run {
//                            if (result.resultCode == RESULT_OK) {
//                                val result =
//                                    GmsDocumentScanningResult.fromActivityResultIntent(result.data)
//                                result?.getPages()?.let { pages ->
//                                    for (page in pages) {
//                                        val imageUri = pages.get(0).getImageUri()
//                                    }
//                                }
//                                result?.getPdf()?.let { pdf ->
//                                    val pdfUri = pdf.getUri()
//                                    val pageCount = pdf.getPageCount()
//                                }
//                            }
//                        }
//                    }
//
//                    scanner.getStartScanIntent(activity)
//                        .addOnSuccessListener { intentSender ->
//                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
//                        }
//                        .addOnFailureListener {
//                            ...
//                        }
                    var imageUris by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }
                    var pdfUri by remember {
                        mutableStateOf<Uri?>(null)
                    }
                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { it ->
                            if (it.resultCode == RESULT_OK) {
                                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                              imageUris = result?.pages?.map { pages -> pages.imageUri }?: emptyList()

                                result?.pdf?.let { pdf ->
                                    pdfUri = pdf.uri
                                }
                            }
                        }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        imageUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                imageUris = emptyList()
                                pdfUri = null
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener { intentSender ->
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(
                                                intentSender
                                            ).build()
                                        )
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Error",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }) {
                                Text(text = "Scan PDF")
                            }
                            pdfUri?.let { uri ->
                                Spacer(modifier = Modifier.width(8.dp))
                                // Button to preview the PDF
                                // Button to preview the PDF
                                Button(onClick = {
                                    try {
                                        // 1. Create a temporary file in the app's cache directory
                                        val tempFile = File(context.cacheDir, "preview_scan.pdf")

                                        // 2. Copy the content from the ML Kit Uri to our temp file
                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                            tempFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }

                                        // 3. Get a safe URI using FileProvider
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )

                                        // 4. Start the Intent
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(contentUri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Could not open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text(text = "Preview")
                                }


                                Spacer(modifier = Modifier.width(8.dp))
                                // Button to save the PDF to Downloads
                                Button(onClick = {
                                    savePdfToDownloads(uri)
                                }) {
                                    Text(text = "Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun savePdfToDownloads(pdfUri: Uri) {
        val fileName = "scan_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    resolver.openInputStream(pdfUri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DocScanTheme {
        Greeting("Android")
    }
}