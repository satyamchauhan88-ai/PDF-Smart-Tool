
package com.pdfsmart.toolkit

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.io.MemoryUsageSetting

object PdfMergeHelper {

    fun merge(
        context: Context,
        pdfUris: List<Uri>,
        outputUri: Uri
    ) {
        require(pdfUris.size >= 2) {
            "Please select at least 2 PDF files"
        }

        PDFBoxResourceLoader.init(context)

        val merger = PDFMergerUtility()

        pdfUris.forEach { uri ->
            val input = context.contentResolver
                .openInputStream(uri)
                ?: throw Exception("Unable to open PDF: $uri")

            merger.addSource(input)
        }

        val output = context.contentResolver
            .openOutputStream(outputUri)
            ?: throw Exception("Unable to create output PDF")

        output.use {
            merger.destinationStream = it

            merger.mergeDocuments(
                MemoryUsageSetting.setupMainMemoryOnly()
            )
        }
    }
}
