package com.example.loomoapp.OpenCV

import android.graphics.Bitmap
import android.util.Log
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.utils.toByteArray
import com.example.loomoapp.utils.toShortArray
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core.*
import org.opencv.core.CvException
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc.*
import java.nio.ByteBuffer

private const val TAG = "CVUtils"

fun ByteBuffer.toMat(width: Int, height: Int, cvType: Int): Mat {
    val mat = Mat()
    mat.create(height, width, cvType)
    when (cvType) {
        CV_8UC4, CV_8UC3, CV_8UC1 -> {
            mat.put(0, 0, this.toByteArray())
        }
        CV_16UC1 -> {
            mat.put(0, 0, this.toShortArray(true))
        }
    }
    return mat
}

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
    } else if (this.type() == CV_16UC1) {
        val tmp = Mat(this.rows(), this.cols(), CV_8UC1)
//        val tmp = Mat(this.dataAddr())
        this.convertTo(tmp, CV_8UC1, 1/256.0)
//        cvtColor(tmp, tmp, COLOR_GRAY2RGB)
//        this.convertTo(this, CV_16UC1, 1/256.0)
//        this.convertTo(tmp, CV_8UC1)
//        normalize(this, tmp, 0.0, 255.0, NORM_MINMAX, CV_8UC1)
//        this.reshape(CV_8UC2)
//        cvtColor(this, tmp, COLOR_BGR5652RGB)
//        tmp.convertTo(tmp, CV_8UC1)
        matToBitmap(tmp, bmp)
        return bmp
    }
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