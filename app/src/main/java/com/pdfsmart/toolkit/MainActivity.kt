
package com.pdfsmart.toolkit

import android.app.Activity
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.android.gms.tasks.Tasks

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument

import java.io.File

class MainActivity : Activity() {

    private lateinit var outputText: TextView
    private lateinit var statusText: TextView

    private var extractedText = ""

    private val requestPdf = 1001
    private val requestSaveText = 1002
    private val requestMergePdf = 1003
    private val requestSaveMergedPdf = 1004

    private val selectedMergePdfs = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(applicationContext)

        createHomeScreen()
    }

    // ---------------------------------------------------------
    // HOME SCREEN
    // ---------------------------------------------------------

    private fun createHomeScreen() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.WHITE)
        }

        val scroll = ScrollView(this)

        val title = TextView(this).apply {
            text = "PDF Smart Toolkit"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Your offline PDF utility"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(title)
        root.addView(subtitle)

        val tools = listOf(
            "MERGE PDF",
            "SPLIT PDF",
            "COMPRESS PDF",
            "PDF TO IMAGES",
            "IMAGES TO PDF",
            "PROTECT PDF",
            "OCR PDF (HINDI + ENGLISH)",
            "SEARCHABLE PDF",
            "EXTRACT TEXT",
            "ROTATE PDF",
            "REORDER PAGES",
            "DELETE PAGES"
        )

        tools.forEach { tool ->

            val button = Button(this).apply {
                text = tool
                textSize = 16f
                isAllCaps = false

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                    bottomMargin = 5
                }
            }

            button.setOnClickListener {

                when (tool) {

                    "MERGE PDF" -> selectPdfsToMerge()

                    "OCR PDF (HINDI + ENGLISH)" -> selectPdf()

                    "EXTRACT TEXT" -> selectPdf()

                    else -> Toast.makeText(
                        this,
                        "$tool will be implemented next",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            root.addView(button)
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 15f
            setTextColor(Color.BLUE)
            setPadding(0, 20, 0, 10)
        }

        root.addView(statusText)

        outputText = TextView(this).apply {
            text = "OCR results will appear here."
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(8, 12, 8, 12)
        }

        root.addView(outputText)

        val copyButton = Button(this).apply {
            text = "COPY EXTRACTED TEXT"
            setOnClickListener {
                copyText()
            }
        }

        val saveButton = Button(this).apply {
            text = "SAVE TEXT AS TXT"
            setOnClickListener {
                saveText()
            }
        }

        root.addView(copyButton)
        root.addView(saveButton)

        scroll.addView(root)

        setContentView(scroll)
    }

    // ---------------------------------------------------------
    // SELECT SINGLE PDF FOR OCR
    // ---------------------------------------------------------

    private fun selectPdf() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivityForResult(intent, requestPdf)
    }

    // ---------------------------------------------------------
    // SELECT MULTIPLE PDF FILES FOR MERGING
    // ---------------------------------------------------------

    private fun selectPdfsToMerge() {

        selectedMergePdfs.clear()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivityForResult(intent, requestMergePdf)
    }

    // ---------------------------------------------------------
    // CREATE DESTINATION FOR MERGED PDF
    // ---------------------------------------------------------

    private fun saveMergedPdf() {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_TITLE,
                "Merged_PDF.pdf"
            )
        }

        startActivityForResult(
            intent,
            requestSaveMergedPdf
        )
    }

    // ---------------------------------------------------------
    // FILE PICKER CALLBACK
    // ---------------------------------------------------------

    @Deprecated("Legacy file picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (resultCode != RESULT_OK || data == null) {
            return
        }

        when (requestCode) {

            requestPdf -> {

                val uri = data.data ?: return

                runOcr(uri)
            }

            requestMergePdf -> {

                selectedMergePdfs.clear()

                val clipData = data.clipData

                if (clipData != null) {

                    for (i in 0 until clipData.itemCount) {

                        selectedMergePdfs.add(
                            clipData.getItemAt(i).uri
                        )
                    }

                } else {

                    data.data?.let {
                        selectedMergePdfs.add(it)
                    }
                }

                if (selectedMergePdfs.size < 2) {

                    Toast.makeText(
                        this,
                        "Select at least 2 PDF files",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }

                saveMergedPdf()
            }

            requestSaveText -> {

                val uri = data.data ?: return

                saveTextToUri(uri)
            }

            requestSaveMergedPdf -> {

                val uri = data.data ?: return

                mergePdfs(uri)
            }
        }
    }

    // ---------------------------------------------------------
    // OCR PDF - ENGLISH + HINDI
    // ---------------------------------------------------------

    private fun runOcr(uri: Uri) {

        statusText.text = "Starting OCR..."
        outputText.text = ""

        Thread {

            var descriptor: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            val latinRecognizer =
                TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )

            val hindiRecognizer =
                TextRecognition.getClient(
                    DevanagariTextRecognizerOptions.Builder().build()
                )

            try {

                descriptor =
                    contentResolver.openFileDescriptor(uri, "r")

                if (descriptor == null) {
                    throw Exception("Unable to open PDF")
                }

                renderer = PdfRenderer(descriptor)

                val result = StringBuilder()

                val totalPages = renderer.pageCount

                for (pageIndex in 0 until totalPages) {

                    runOnUiThread {

                        statusText.text =
                            "Processing page ${pageIndex + 1} of $totalPages..."
                    }

                    val page = renderer.openPage(pageIndex)

                    var bitmap: Bitmap? = null

                    try {

                        val scale = 2

                        bitmap = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )

                        bitmap.eraseColor(Color.WHITE)

                        page.render(
                            bitmap,
                            Rect(
                                0,
                                0,
                                bitmap.width,
                                bitmap.height
                            ),
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        val image =
                            InputImage.fromBitmap(bitmap, 0)

                        val englishResult =
                            Tasks.await(
                                latinRecognizer.process(image)
                            )

                        val hindiResult =
                            Tasks.await(
                                hindiRecognizer.process(image)
                            )

                        result.append("\n\n")
                        result.append(
                            "===== PAGE ${pageIndex + 1} =====\n\n"
                        )

                        result.append(englishResult.text)

                        if (hindiResult.text.isNotBlank()) {

                            result.append("\n")
                            result.append(hindiResult.text)
                        }

                    } finally {

                        bitmap?.recycle()
                        page.close()
                    }
                }

                extractedText = result.toString()

                runOnUiThread {

                    outputText.text = extractedText

                    statusText.text =
                        "OCR completed. Pages processed: $totalPages"

                    Toast.makeText(
                        this,
                        "OCR completed successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    statusText.text = "OCR failed"

                    outputText.text =
                        "Error: ${e.message}"

                    Toast.makeText(
                        this,
                        "OCR error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                renderer?.close()
                descriptor?.close()

                latinRecognizer.close()
                hindiRecognizer.close()
            }

        }.start()
    }

    // ---------------------------------------------------------
    // MERGE PDF FILES
    // ---------------------------------------------------------

    private fun mergePdfs(outputUri: Uri) {

        if (selectedMergePdfs.size < 2) {

            Toast.makeText(
                this,
                "Please select at least 2 PDF files",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        statusText.text = "Merging PDFs..."

        Thread {

            var mergedDocument: PDDocument? = null

            try {

                mergedDocument = PDDocument()

                for (uri in selectedMergePdfs) {

                    val tempFile = File.createTempFile(
                        "pdf_merge_",
                        ".pdf",
                        cacheDir
                    )

                    try {

                        contentResolver.openInputStream(uri)?.use { input ->

                            tempFile.outputStream().use { output ->

                                input.copyTo(output)
                            }

                        } ?: throw Exception(
                            "Cannot read selected PDF"
                        )

                        val sourceDocument =
                            PDDocument.load(tempFile)

                        try {

                            for (
                                pageIndex in 0 until
                                sourceDocument.numberOfPages
                            ) {

                                val page =
                                    sourceDocument.getPage(pageIndex)

                                mergedDocument.importPage(page)
                            }

                        } finally {

                            sourceDocument.close()
                        }

                    } finally {

                        tempFile.delete()
                    }
                }

                contentResolver.openOutputStream(outputUri)?.use {

                    mergedDocument.save(it)

                } ?: throw Exception(
                    "Cannot save merged PDF"
                )

                runOnUiThread {

                    statusText.text = "PDF merge completed"

                    Toast.makeText(
                        this,
                        "PDFs merged successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    statusText.text = "PDF merge failed"

                    Toast.makeText(
                        this,
                        "Merge error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                mergedDocument?.close()
            }

        }.start()
    }

    // ---------------------------------------------------------
    // COPY EXTRACTED TEXT
    // ---------------------------------------------------------

    private fun copyText() {

        if (extractedText.isBlank()) {

            Toast.makeText(
                this,
                "No extracted text available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val clipboard =
            getSystemService(CLIPBOARD_SERVICE)
                as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "OCR Text",
                extractedText
            )
        )

        Toast.makeText(
            this,
            "Text copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------------------------------------------------
    // SAVE TEXT AS TXT
    // ---------------------------------------------------------

    private fun saveText() {

        if (extractedText.isBlank()) {

            Toast.makeText(
                this,
                "Please run OCR first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {

                addCategory(Intent.CATEGORY_OPENABLE)

                type = "text/plain"

                putExtra(
                    Intent.EXTRA_TITLE,
                    "OCR_Result.txt"
                )
            }

        startActivityForResult(
            intent,
            requestSaveText
        )
    }

    // ---------------------------------------------------------
    // WRITE EXTRACTED TEXT TO FILE
    // ---------------------------------------------------------

    private fun saveTextToUri(uri: Uri) {

        try {

            contentResolver.openOutputStream(uri)?.use {

                it.write(
                    extractedText.toByteArray(
                        Charsets.UTF_8
                    )
                )

            } ?: throw Exception(
                "Unable to open output file"
            )

            Toast.makeText(
                this,
                "Text saved successfully",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Save failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
