package com.hbde.courseschedule.ocr.scanner

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

/**
 * ML Kit OCR 封装
 * 中文识别使用 ChineseTextRecognizerOptions
 */
class OcrScanner {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    /**
     * 处理图片并返回识别到的文本结果
     */
    fun processImage(bitmap: Bitmap, onSuccess: (Text) -> Unit, onFailure: (Exception) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { text ->
                onSuccess(text)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * 同步风格接口（如需协程包裹可在调用处扩展）
     */
    fun processImage(bitmap: Bitmap): Text? {
        // TODO: 如需同步/挂起函数封装，可在此补充 Kotlin Coroutines 适配
        return null
    }
}
