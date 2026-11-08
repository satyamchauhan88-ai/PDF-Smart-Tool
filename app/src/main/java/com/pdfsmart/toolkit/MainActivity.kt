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
import org.apache.pdfbox.pdmodel.PDDocument

class MainActivity : Activity() {

    private lateinit var outputText: TextView
    private lateinit var statusText: TextView

    private var extractedText = ""

    private val requestPdf = 1001
    private val requestSaveText = 1002
    private val requestMergePdf = 1003
    private val requestSaveMergedPdf = 1004

    private var selectedMergePdfs = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(applicationContext)

        createHomeScreen()
    }

    // ================= HOME SCREEN =================

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
            setOnClickListener { copyText() }
        }

        val saveButton = Button(this).apply {
            text = "SAVE TEXT AS TXT"
            setOnClickListener { saveText() }
        }

        root.addView(copyButton)
        root.addView(saveButton)

        scroll.addView(root)

        setContentView(scroll)
    }

    // ================= SELECT PDF FOR OCR =================

    private fun selectPdf() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }

        startActivityForResult(intent, requestPdf)
    }

    // ================= SELECT MULTIPLE PDFS =================

    private fun selectPdfsToMerge() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        startActivityForResult(intent, requestMergePdf)
    }

    // ================= SAVE MERGED PDF =================

    private fun saveMergedPdf() {

        if (selectedMergePdfs.size < 2) {

            Toast.makeText(
                this,
                "Please select at least 2 PDF files",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "Merged_PDF.pdf")
        }

        startActivityForResult(intent, requestSaveMergedPdf)
    }

    // ================= ACTIVITY RESULT =================

    @Deprecated("Legacy file picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(requestCode, resultCode, data)

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

                if (selectedMergePdfs.size >= 2) {

                    Toast.makeText(
                        this,
                        "${selectedMergePdfs.size} PDFs selected",
                        Toast.LENGTH_LONG
                    ).show()

                    saveMergedPdf()

                } else {

                    Toast.makeText(
                        this,
                        "Please select at least 2 PDFs",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            requestSaveText -> {

                val uri = data.data ?: return

                try {

                    contentResolver.openOutputStream(uri)?.use {
                        it.write(
                            extractedText.toByteArray(Charsets.UTF_8)
                        )
                    }

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

            requestSaveMergedPdf -> {

                val uri = data.data ?: return

                mergePdfs(uri)
            }
        }
    }

    // ================= MERGE PDF FILES =================

    private fun mergePdfs(outputUri: Uri) {

        statusText.text = "Merging PDFs..."

        Thread {

            var outputDocument: PDDocument? = null

            try {

                outputDocument = PDDocument()

                for (pdfUri in selectedMergePdfs) {

                    contentResolver.openInputStream(pdfUri)?.use { input ->

                        PDDocument.load(input).use { sourceDocument ->

                            for (pageIndex in 0 until sourceDocument.numberOfPages) {

                                outputDocument.addPage(
                                    sourceDocument.getPage(pageIndex)
                                )
                            }
                        }

                    } ?: throw Exception("Unable to open selected PDF")
                }

                contentResolver.openOutputStream(outputUri)?.use { output ->

                    outputDocument.save(output)
                } ?: throw Exception("Unable to create output PDF")

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

                outputDocument?.close()
            }

        }.start()
    }

    // ================= OCR PDF =================

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

                descriptor = contentResolver.openFileDescriptor(uri, "r")

                    ?: throw Exception("Unable to open PDF")

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

                        val width = page.width * scale
                        val height = page.height * scale

                        bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                        bitmap.eraseColor(Color.WHITE)

                        page.render(
                            bitmap,
                            Rect(0, 0, width, height),
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        val image = InputImage.fromBitmap(bitmap, 0)

                        val englishResult =
                            Tasks.await(
                                latinRecognizer.process(image)
                            )

                        val hindiResult =
                            Tasks.await(
                                hindiRecognizer.process(image)
                            )

                        result.append("\n\n")
                        result.append("===== PAGE ${pageIndex + 1} =====\n\n")

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

    // ================= COPY TEXT =================

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
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

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

    // ================= SAVE OCR TEXT =================

    private fun saveText() {

        if (extractedText.isBlank()) {

            Toast.makeText(
                this,
                "Please run OCR first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "OCR_Result.txt")
        }

        startActivityForResult(intent, requestSaveText)
    }
}
