package com.example.loomoapp.OpenCV

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.CvException
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc.*
import java.nio.ByteBuffer

private const val TAG = "CVUtils"

//fun ByteBuffer.toMat(width: Int, height: Int, cvType: Int): Mat {
//    val mat = Mat()
//    mat.create(height, width, cvType)
//    when (cvType) {
//        CV_8UC4, CV_8UC3, CV_8UC2, CV_8UC1 -> {
//            mat.put(0, 0, this.toByteArray())
//        }
//        CV_16UC1 -> {
//            mat.put(0, 0, this.toShortArray(true))
//        }
//    }
//    return mat
//}
fun ByteBuffer.toMat(width: Int, height: Int, cvType: Int): Mat {
    //TODO: sanity-check the Mat()-construction (e.g. with try/catch)
    return Mat(height, width, cvType, this)
}

fun Array<Double>.toMat(rows: Int = 1, cols: Int = this.size): Mat {
    val mat = Mat(rows, cols, CV_64FC1)
    if (this.size != rows*cols) {
        return mat
    }
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val arrVal = when{
                row*cols + col > this.size -> {
                    0.0
                }
                else -> {
                    this[row*cols + col]
                }
            }
            mat.put(row, col, arrVal)
        }
    }
    return mat
}

//fun <T> Array<T>.toMat(rows: Int = 1, cols: Int = this.size): Mat {
//    when(this[0]) {
//        is Int -> {val cvType = CV_32SC1}
//        else -> {val cvType = CV_8UC1}
//    }
//}

fun Mat.toBitmap(): Bitmap {
    if ((this.cols() == 0) or (this.rows() == 0)) {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    //TODO: 'this' appears to be volatile because the next line will sometimes try to
    // create an empty Bitmap despite the check in the previous line
    val bmp = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)

    // matToBitmap() only works for CV_8UC1, CV_8UC3 and CV_8UC4 formats,
    if (this.type() == CV_8UC2) {
        // The color space conversion for some reason does not change the frame.type(),
        // so the workaround is make a new 'Mat' with a usable type
        val tmp = Mat(this.rows(), this.cols(), CV_8UC3)
        cvtColor(this, tmp, COLOR_BGR5652RGB)
        matToBitmap(tmp, bmp)
        return bmp
    }
//    else if (this.type() == CV_16UC1) {
//        val tmp = Mat(this.rows(), this.cols(), CV_8UC1)
//        //Fix the conversion of depth img to gray-scale. All attempts seem to
//        // 'cut off' the MSB instead of mapping 0-2^16 to 0-2^8
//        // Logging the pixel values suggests that the 'looping' effect is happening in
//        // the 16bit img, so the problem lies elsewhere
////        Imgproc.blur(this, this, Size(9.0, 9.0))
////        Log.d(TAG, "ch: ${this.channels()}, depth: ${this.depth()}, val: ${this[120, 155][0]}")
////        val tmp = Mat(this.dataAddr())
//        this.convertTo(tmp, CV_8UC1, 1/256.0)
////        cvtColor(tmp, tmp, COLOR_GRAY2RGB)
////        this.convertTo(this, CV_16UC1, 1/256.0)
////        this.convertTo(tmp, CV_8UC1)
////        normalize(this, tmp, 0.0, 255.0, NORM_MINMAX, CV_8UC1)
////        this.reshape(CV_8UC2)
////        cvtColor(this, tmp, COLOR_BGR5652RGB)
////        tmp.convertTo(tmp, CV_8UC1)
//        matToBitmap(tmp, bmp)
//        return bmp
//    }
//        if (!(frame.type() == CV_8UC1 || frame.type() == CV_8UC3 || frame.type() == CV_8UC4)) {
//        }
    if (this.empty()) {
        Log.d(TAG, "Mat().toBitmap: Frame is empty")
        return bmp
    }
    try {
        matToBitmap(this, bmp)
    } catch (e: CvException) {
        return bmp
    }
    return bmp
}

//TODO: It could be practical if Mat() had an iterator method (https://kotlinlang.org/docs/reference/iterators.html)
// Maybe this tutorial can help: https://www.baeldung.com/kotlin-custom-range-iterator


//fun Mat.toIntArray(): IntArray {
//    if (this.channels() + this.depth() > 4) {
//        throw MatConversionException("Too many bits per pixel in 'Mat.toIntArray'")
//    }
//
//}
//
//class MatConversionException(msg: String) : RuntimeException(msg)

