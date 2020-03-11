package com.example.loomoapp.OpenCV

import android.graphics.Bitmap
import android.util.Log
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.utils.toByteArray
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.CvException
import org.opencv.core.CvType.CV_8UC2
import org.opencv.core.CvType.CV_8UC3
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc.COLOR_BGR5652RGB
import org.opencv.imgproc.Imgproc.cvtColor
import java.nio.ByteBuffer

private const val TAG = "CVUtils"

fun ByteBuffer.toMat(width: Int, height: Int, cvType: Int): Mat {
    val mat = Mat()
    mat.create(height, width, cvType)
    mat.put(0, 0, this.toByteArray())
    return mat
}

fun Mat.toBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    // matToBitmap() only works for CV_8UC1, CV_8UC3 and CV_8UC4 formats,
    // so the depth image's color space must be converted
    if (this.type() == CV_8UC2) {
        // The color space conversion for some reason does not change the frame.type(),
        // so the workaround is make a new 'Mat' with a usable type
        val tmp = Mat(DEPTH_HEIGHT, DEPTH_WIDTH, CV_8UC3)
        cvtColor(this, tmp, COLOR_BGR5652RGB)
        matToBitmap(tmp, bmp)
        return bmp
    }
//        if (!(frame.type() == CV_8UC1 || frame.type() == CV_8UC3 || frame.type() == CV_8UC4)) {
//        }
    if (this.empty()) {
        Log.d(TAG, "Frame is empty")
        return bmp
    }
    try {
        matToBitmap(this, bmp)
    } catch (e: CvException) {
        return bmp
    }
    return bmp
}