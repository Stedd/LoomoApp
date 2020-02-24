package com.example.loomoapp.OpenCV

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.example.loomoapp.R
import com.example.loomoapp.TAG
import org.opencv.android.*
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.ORB

class OpenCVMain(): Activity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var mLoaderCallback: BaseLoaderCallback




    private val detectorAndroidCam: ORB = ORB.create(10, 1.9F)
    private val keypointsAndroidCam = MatOfKeyPoint()


    var img = Mat()
    var imgFisheye = Mat()
    var resultImg = Mat()
    var resultImgFisheye = Mat()

    fun init(){
        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded")
        } else {
            Log.d(TAG, "OpenCV loaded")
        }
    }

    fun onCreate(context: Context){
        //Initialize OpenCV camera view
        val mCameraView = context.findviewbyid<JavaCameraView>(R.id.javaCam)
        mCameraView.setCameraPermissionGranted()
        mCameraView.visibility = SurfaceView.INVISIBLE
        mCameraView.setCameraIndex(-1)
//        mCameraView.enableFpsMeter()
        mCameraView.setCvCameraViewListener(this)

        mLoaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.i(com.example.loomoapp.TAG, "OpenCV loaded successfully, enabling camera view")
                        mCameraView.enableView()
                        mCameraView.visibility = SurfaceView.VISIBLE
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




    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCameraViewStopped() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}