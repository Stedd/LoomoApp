package com.example.loomoapp.OpenCV

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.toByteArray
import org.opencv.android.*
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import java.nio.ByteBuffer

class OpenCVMain: Service() {
    private val TAG = "OpenCVClass"

    init {
        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded")
        } else {
            Log.d(TAG, "OpenCV loaded")
        }
    }

    private lateinit var mLoaderCallback: BaseLoaderCallback
//    private var frame = Mat()
    private var fishEyeFrame = Mat()
    private var colorFrame = Mat()
    private var depthFrame = Mat()

    private val detectorAndroidCam: ORB = ORB.create(10, 1.9F)
    private val keypointsAndroidCam = MatOfKeyPoint()
//
    private var img = Mat()
//    private var imgFisheye = Mat()
//    private var resultImg = Mat()
//    private var resultImgFisheye = Mat()

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    fun onCreate(context: Context) {
        mLoaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.d(TAG, "OpenCV loaded successfully")
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }
    }

    fun resume(){
        //Start OpenCV
        Log.d(TAG, "Activity resumed")
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

//    fun newFrame(byteBuf: ByteBuffer) {
//        frame.create(FISHEYE_HEIGHT, FISHEYE_WIDTH, CvType.CV_8UC1)
//        frame.put(0,0, byteBuf.toByteArray())
//    }


//    fun getFrame(): Bitmap {
//        val conf = when (frame.channels()) {
////            1 -> Bitmap.Config.ALPHA_8 // not allowed bmp type
//            2 -> Bitmap.Config.RGB_565
//            else -> Bitmap.Config.ARGB_8888
//        }
//        val bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), conf)
//        matToBitmap(frame, bmp)
//        return bmp
//    }

    fun newFishEyeFrame(byteBuf: ByteBuffer) {
        newFrame(byteBuf, fishEyeFrame, FISHEYE_WIDTH, FISHEYE_HEIGHT, CvType.CV_8UC1)
    }
    fun newColorFrame(byteBuf: ByteBuffer) {
        newFrame(byteBuf, colorFrame, COLOR_WIDTH, COLOR_HEIGHT, CvType.CV_8UC4)
    }
    fun newDepthFrame(byteBuf: ByteBuffer) {
        newFrame(byteBuf, depthFrame, DEPTH_WIDTH, DEPTH_HEIGHT, CvType.CV_8UC2)
    }

    fun getFishEyeFrame(): Bitmap {
        return getFrame(fishEyeFrame)
    }
    fun getColorFrame(): Bitmap {
        return getFrame(colorFrame)
    }
    fun getDepthFrame(): Bitmap {
        return getFrame(depthFrame)
    }

    private fun newFrame(byteBuf: ByteBuffer, dst: Mat, width: Int, height: Int, cvType: Int) {
        dst.create(height, width, cvType)
        dst.put(0, 0, byteBuf.toByteArray())
    }

    private fun getFrame(frame: Mat): Bitmap {
//        val conf = when (frame.channels()) {
//            2 -> Bitmap.Config.ARGB_8888
//            else -> Bitmap.Config.ARGB_8888
//        }
//
//        if (frame.channels() == 2) {
//            val tmp = Mat()
//            cvtColor(frame, tmp, COLOR_BGR5652RGB) // this is super slow for some reason
//            val bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), conf)
//            matToBitmap(tmp, bmp)
//            return bmp
//        }

        val bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888)
        matToBitmap(frame, bmp)
        return bmp
    }
}