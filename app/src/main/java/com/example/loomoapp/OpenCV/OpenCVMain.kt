package com.example.loomoapp.OpenCV

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.SurfaceView
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import org.opencv.android.*
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.features2d.ORB
import java.nio.ByteBuffer

//class OpenCVMain: Service(), CameraBridgeViewBase.CvCameraViewListener2 {
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
    private var frame = Mat()

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

//    fun onCreate(context: Context, camFrame: JavaCameraView) {
//        mLoaderCallback = object : BaseLoaderCallback(context) {
//            override fun onManagerConnected(status: Int) {
//                when (status) {
//                    LoaderCallbackInterface.SUCCESS -> {
//                        Log.d(TAG, "OpenCV loaded successfully")
//                    }
//                    else -> {
//                        super.onManagerConnected(status)
//                    }
//                }
//            }
//        }
//    }
    fun onCreate(context: Context, camFrame: JavaCameraView) {
        //Initialize OpenCV camera view
        camFrame.setCameraPermissionGranted()
        camFrame.visibility = SurfaceView.INVISIBLE
        camFrame.setCameraIndex(-1)
//        camFrame.setCvCameraViewListener(this)

        mLoaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.i(TAG, "OpenCV loaded successfully, enabling camera view")
                        camFrame.enableView()
                        camFrame.visibility = SurfaceView.VISIBLE
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

    fun newFrame(byteBuf: ByteBuffer) {
        frame.create(FISHEYE_HEIGHT, FISHEYE_WIDTH, CvType.CV_8UC1)
        frame.put(0,0, getByteBufferAsByteArray(byteBuf))
    }

    private fun getByteBufferAsByteArray(src: ByteBuffer): ByteArray {
        val bytesInBuffer = src.remaining()
        val tmpArr = ByteArray(bytesInBuffer) { src.get() }
        src.rewind()
        return tmpArr
    }

    fun getFrame(): Bitmap {
        val conf = when (frame.channels()) {
//            1 -> Bitmap.Config.ALPHA_8 // not allowed bmp type
            2 -> Bitmap.Config.RGB_565
            else -> Bitmap.Config.ARGB_8888
        }
        val bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), conf)
        matToBitmap(frame, bmp)
        return bmp
    }


//    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//        return inputFrame.rgba()
//    }
//
//    override fun onCameraViewStarted(width: Int, height: Int) {
//
//    }
//
//    override fun onCameraViewStopped() {
//    }

}