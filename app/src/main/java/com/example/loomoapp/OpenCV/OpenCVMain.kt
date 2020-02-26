package com.example.loomoapp.OpenCV

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.SurfaceView
import org.opencv.android.*
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.ORB

class OpenCVMain: Service(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val TAG = "OpenCV"

    init {
        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded")
        } else {
            Log.d(TAG, "OpenCV loaded")
        }
    }

    private lateinit var mLoaderCallback: BaseLoaderCallback

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

    fun onCreate(context: Context, camFrame: JavaCameraView){
        //Initialize OpenCV camera view
        camFrame.setCameraPermissionGranted()
        camFrame.visibility = SurfaceView.INVISIBLE
        camFrame.setCameraIndex(-1)
        camFrame.setCvCameraViewListener(this)

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
        Log.i(TAG, "Activity resumed")
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        return inputFrame.gray()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {
    }

}