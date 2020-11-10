@file:DependsOn("org.apache.pdfbox:pdfbox:2.0.21")

import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSFloat
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup
import org.apache.pdfbox.text.PDFTextStripperByArea
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.FileWriter
import java.io.IOException

val MD_HEADER_1_TEMPLATE = "# %s"
val MD_HEADER_3_TEMPLATE = "##### %s"
val MD_BULLET_LIST_ITEM = "* %s"

val HIGHLIGHTED_TEXT_TEMPLATE = "*Highlight*: %s"
val HIGHLIGHTED_TEXT_WITH_COMMENT_TEMPLATE = "*Highlight*: %s\n\t(*Comment:* %s)"
val STICKY_NOTE_TEMPLATE = "*Sticky note:* %s"

execute(args)

fun execute(arguments: Array<String>) {
    if (arguments.size != 2) {
        printError("Wrong number of arguments. PDF file path and destination file path should be provided.")
        return
    }

    val pdfFile = File(arguments[0])
    if (!pdfFile.exists()) {
        printError("PDF file doesn't exist: ${pdfFile.absoluteFile}")
        return
    }

    val destinationFile = File(arguments[1])
    val isDestinationFileCreated = try {
        destinationFile.createNewFile()
    } catch (ioException: IOException) {
        false
    }
    if (!isDestinationFileCreated) {
        printError("Error creating destination file: ${arguments[1]}. File might already exists.")
        return
    }

    val pdfDocument = PDDocument.load(pdfFile)
    val destinationFileWriter = FileWriter(destinationFile, true)

    val documentTitle =
            if (!pdfDocument.documentInformation.title.isNullOrBlank()) pdfDocument.documentInformation.title else pdfFile.name
    destinationFileWriter.appendln(format(MD_HEADER_1_TEMPLATE, documentTitle))
    if (!pdfDocument.documentInformation.author.isNullOrBlank()) {
        destinationFileWriter.appendln("(${pdfDocument.documentInformation.author})")
    }

    processDocument(pdfDocument, destinationFileWriter)

    pdfDocument.close()
    destinationFileWriter.close()
}

fun processDocument(pdfDocument: PDDocument, destinationFile: FileWriter) {
    pdfDocument.pages.forEachIndexed { pageNo, page ->
        println("Processing page #${pageNo + 1}")
        val pageAnnotations = page.annotations.filter { isAnnotationProcessable(it) }
        if (pageAnnotations.isNotEmpty()) {
            format(MD_HEADER_3_TEMPLATE, "Page ${pageNo + 1}")?.let { destinationFile.appendln(it) }
            pageAnnotations.forEach {
                val annotationText = convertAnnotationToString(it)
                if (!annotationText.isNullOrBlank()) {
                    format(MD_BULLET_LIST_ITEM, annotationText)?.let { destinationFile.appendln(it) }
                }
            }
        }
    }
}

fun isAnnotationProcessable(annotation: PDAnnotation) = AnnotationType.contains(annotation.subtype)

fun convertAnnotationToString(annotation: PDAnnotation): String? =
        when {
            // annotation.contents.isBlank() to filter out highlights with popup.
            AnnotationType.HIGHLIGHT.isEqualTo(annotation.subtype) && annotation.contents.isNullOrBlank() -> format(
                    HIGHLIGHTED_TEXT_TEMPLATE,
                    getHighlightedRegionText(annotation)
            )
            AnnotationType.POPUP.isEqualTo(annotation.subtype) -> getPopupAnnotationText((annotation as PDAnnotationPopup).parent)
            else -> null
        }

fun getPopupAnnotationText(annotationMarkup: PDAnnotationMarkup) =
        when {
            // "Sticky note" annotation
            "Text" == annotationMarkup.subtype -> {
                format(STICKY_NOTE_TEMPLATE, annotationMarkup.contents)
            }
            AnnotationType.HIGHLIGHT.isEqualTo(annotationMarkup.subtype) -> format(
                    HIGHLIGHTED_TEXT_WITH_COMMENT_TEMPLATE,
                    getHighlightedRegionText(annotationMarkup),
                    annotationMarkup.contents // User comment
            )
            else -> null
        }

fun getHighlightedRegionText(annotation: PDAnnotation): String {
    val stringBuilder = StringBuilder()
    val quadPoints = annotation.cosObject.getDictionaryObject(COSName.QUADPOINTS) as COSArray
    for (i in 0 until quadPoints.size() step 8) {
        val topLeftX = (quadPoints.get(i + 0) as COSFloat).floatValue()
        val topLeftY = (quadPoints.get(i + 1) as COSFloat).floatValue()
        val topRightX = (quadPoints.get(i + 2) as COSFloat).floatValue()
        val bottomLeftY = (quadPoints.get(i + 5) as COSFloat).floatValue()
        val width = topRightX - topLeftX
        val height = topLeftY - bottomLeftY
        val displayY = annotation.page.mediaBox.height - topLeftY
        val rect = Rectangle2D.Float(topLeftX, displayY, width, height)

        val pdfTextStripperByArea = PDFTextStripperByArea()
        pdfTextStripperByArea.addRegion("region", rect)
        pdfTextStripperByArea.extractRegions(annotation.page)
        stringBuilder.append(pdfTextStripperByArea.getTextForRegion("region"))
    }
    return stringBuilder.toString()
}

fun printError(message: String) {
    System.err.println("Error: $message")
}

fun format(template: String, vararg arguments: String?) =
        if (arguments.any { it == null }) null else template.format(*arguments)

private enum class AnnotationType {
    HIGHLIGHT, POPUP;

    fun isEqualTo(value: String) = name.toLowerCase() == value.toLowerCase()

    companion object {
        fun contains(value: String) = values().any { it.name.toLowerCase() == value.toLowerCase() }
    }
}