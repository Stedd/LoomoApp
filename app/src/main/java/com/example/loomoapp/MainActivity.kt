package com.example.loomoapp

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

//Variables
const val TAG = "debugMSG"

lateinit var viewModel: MainActivityViewModel
lateinit var mBase: Base
lateinit var mVision: Vision
lateinit var mLoomoSensor: LoomoSensor
lateinit var mLoaderCallback: BaseLoaderCallback


val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread
var cameraRunning: Boolean = false

var img = Mat()
var resultImg = Mat()

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    external fun stringFromJNI(): String

    init {
        //Load native
        System.loadLibrary("native-lib")

        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded")
        } else {
            Log.d(TAG, "OpenCV loaded")
        }
    }

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    private val imgWidth = 640
    private val imgHeight = 480
    private var mImgDepth = Bitmap.createBitmap(
        imgWidth,
        imgHeight,
        Bitmap.Config.ALPHA_8
    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels

    private var mImgDepthScaled = Bitmap.createScaledBitmap(mImgDepth, imgWidth/2, imgHeight/2,false)


    private val mDistanceController = DistanceController()
    private var mControllerThread = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")

        val mCameraView = findViewById<JavaCameraView>(R.id.javaCam)
        mCameraView.setCameraPermissionGranted()
        mCameraView.visibility = SurfaceView.INVISIBLE
        mCameraView.setCameraIndex(-1)
//        mCameraView.enableFpsMeter()
        mCameraView.setCvCameraViewListener(this)

        mLoaderCallback = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.i(TAG, "OpenCV loaded successfully, enabling camera view")
                        mCameraView.enableView()
                        mCameraView.visibility = SurfaceView.VISIBLE
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }

        mBase = Base.getInstance()
        mVision = Vision.getInstance()
        mLoomoSensor = LoomoSensor(this)

        viewModel = ViewModelProvider(this)
            .get(MainActivityViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))

        viewModel.text.value = "Service not started"

        btnStartService.setOnClickListener {
            startController("ControllerThread start command")
//            Log.d(TAG, "${HelloWorld()}")
        }
        btnStopService.setOnClickListener {
            stopController("ControllerThread stop command")
        }
        btnStartCamera.setOnClickListener {
            startCamera("Camera start command")
        }
        btnStopCamera.setOnClickListener {
            stopCamera("Camera stop command")
        }

        sample_text.text = stringFromJNI()
//        Log.i(TAG, "from c++ ${stringFromJNI()}")
    }

    override fun onResume() {
        Log.i(TAG, "Activity resumed")
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }

        //Bind loomo services
        mBase.bindService(this.applicationContext, object : ServiceBinder.BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Base onBind")
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Base unBind. Reason: $reason")
            }
        })

        mVision.bindService(this.applicationContext, object : ServiceBinder.BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Vision onBind")
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Vision unBind. Reason $reason")
            }
        })

        super.onResume()
    }

    override fun onDestroy() {
        stopThreads()
        super.onDestroy()
    }

    override fun onPause() {
        stopThreads()
        super.onPause()
    }

    private fun stopThreads() {
        stopController("App paused, Controller thread stopping")

        stopCamera("App paused, Camera thread stopping")

    }

    private fun startController(msg: String) {
        if (!mControllerThread.isAlive) {
            Log.i(TAG, msg)
            mDistanceController.enable = true
            mControllerThread = Thread(mDistanceController, "ControllerThread")
            mControllerThread.start()
        } else {
            Toast.makeText(this, "Dude, the controller is already activated..", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun stopController(msg: String) {
        if (mControllerThread.isAlive) {
            Log.i(TAG, msg)
            mDistanceController.enable = false
        }
    }

    private fun startCamera(msg: String) {
        if (mVision.isBind) {
            if (!cameraRunning) {
                Log.i(TAG, msg)
                mVision.startListenFrame(StreamType.FISH_EYE) { streamType, frame ->
                    mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
                    mImgDepthScaled = Bitmap.createScaledBitmap(mImgDepth, imgWidth/2, imgHeight/2,false)
                    threadHandler.post {
                        camView.setImageBitmap(mImgDepthScaled.copy(Bitmap.Config.ALPHA_8, true))
                    }
                }
                cameraRunning = true
            } else {
                Toast.makeText(this, "Dude, the camera is already activated..", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "Vision service not started yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera(msg: String) {
        if (cameraRunning) {
            Log.i(TAG, msg)
            mVision.stopListenFrame(StreamType.FISH_EYE)
            cameraRunning = false
            camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))
        }
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//        Log.i("cam", "new frame")
        img = inputFrame.gray()
//        resultImg = img
//        Imgproc.blur(img, resultImg, Size(15.0, 15.0))
//        Imgproc.GaussianBlur(img, resultImg, Size(15.0, 15.0), 1.0)
        Imgproc.Canny(img, resultImg, 0.01, 190.0)
//        Log.i("cam", "${Thread.currentThread()}")

        return resultImg
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.i("cam", "camera view started. width:$width, height: $height")
        img = Mat(width, height, CvType.CV_8UC4)
}

    override fun onCameraViewStopped() {
        Log.i("cam", "camera view stopped")
    }
}
