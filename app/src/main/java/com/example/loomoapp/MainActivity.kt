package com.example.loomoapp

import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.ComputerVision.CVHandlerThread
import com.example.loomoapp.ComputerVision.CVHandlerThread.Companion.TASK1
import com.example.loomoapp.viewModel.MainActivityViewModel
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.core.Core.NORM_HAMMING
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.Features2d
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc


//Variables
const val TAG = "MainActivity"

lateinit var viewModel: MainActivityViewModel
lateinit var mBase: Base
lateinit var mVision: Vision
lateinit var mLoomoSensor: LoomoSensor
lateinit var mLoaderCallback: BaseLoaderCallback


val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread
var cameraRunning: Boolean = false



var img = Mat()
var imgFisheye = Mat()
var resultImg = Mat()
var resultImgFisheye = Mat()
var orbKeyPointsOld = MatOfKeyPoint()
var orbDescriptorsOld = Mat()
var imgOld = Mat()
var trainList = listOf<KeyPoint>()

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

//    val cvHandlerTread = HandlerThread("CVThread", Process.THREAD_PRIORITY_FOREGROUND)

    val cvHandlerTread = CVHandlerThread("CVThread", Process.THREAD_PRIORITY_BACKGROUND)

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

    private var captureNewKeyFrame = true
    private val imgWidth = 640
    private val imgHeight = 480
    private var mImgDepth = Bitmap.createBitmap(
        imgWidth,
        imgHeight,
        Bitmap.Config.ARGB_8888
    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels
    private var mImgDepthCanny = Bitmap.createBitmap(
        imgWidth/3,
        imgHeight/3,
        Bitmap.Config.ARGB_8888
    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels

    private var mImgDepthScaled = Bitmap.createScaledBitmap(mImgDepth, imgWidth/3, imgHeight/3,false)


    private val mDistanceController = DistanceController()
    private var mControllerThread = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        Log.i(TAG, "Activity created")

        cvHandlerTread.start()

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

//        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))

        viewModel.text.value = "Service not started"

//        btnStartService.setOnClickListener {
//            startController("ControllerThread start command")
////            Log.d(TAG, "${HelloWorld()}")
//        }
//        btnStopService.setOnClickListener {
//            stopController("ControllerThread stop command")
//        }
        btnCaptureFrame.setOnClickListener {
//            startCamera("Camera start command")
            captureNewKeyFrame = true
        }
        btnStopCamera.setOnClickListener {
            cvHandlerTread.handler.post {
                Log.d(TAG, "Runnable.")
            }
            val msg = Message.obtain(cvHandlerTread.handler)
            msg.what = TASK1
            cvHandlerTread.handler.sendMessage(msg)
            val msg2 = Message.obtain(cvHandlerTread.handler)
            msg2.what = 7
            cvHandlerTread.handler.sendMessage(msg2)
//            stopCamera("Camera stop command")
        }

//        sample_text.text = stringFromJNI()
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
                    mImgDepthScaled = Bitmap.createScaledBitmap(mImgDepth, imgWidth/3, imgHeight/3,false)
                    Utils.bitmapToMat(mImgDepthScaled, imgFisheye)
                    Imgproc.Canny(imgFisheye, resultImgFisheye, 0.01, 190.0)
                    Utils.matToBitmap(resultImgFisheye ,mImgDepthCanny)

                    threadHandler.post {
//                        camView.setImageBitmap(mImgDepth.copy(Bitmap.Config.ARGB_8888, true))
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
//            camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))
        }
    }


    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        img = inputFrame.gray()
//        resultImg = img
//        Imgproc.blur(img, resultImg, Size(15.0, 15.0))
//        Imgproc.GaussianBlur(img, resultImg, Size(15.0, 15.0), 1.0)
//        Imgproc.Canny(img, resultImg, 0.01, 190.0)

//        val detector: ORB = ORB.create(50, 1.2F)
//        val keypoints = MatOfKeyPoint()
//        detector.detect(img, keypoints)
//
//

        val detector: ORB = ORB.create()
        val keyPoints = MatOfKeyPoint()
        val descriptors = Mat()
        detector.detect(img, keyPoints)
        detector.compute(img, keyPoints, descriptors)

//        Features2d.drawKeypoints(img, keyPoints, resultImg)

        if (captureNewKeyFrame) {
            //Update the keypoints used for comparison
            captureNewKeyFrame = false
            detector.detect(img, orbKeyPointsOld)
            detector.compute(img, orbKeyPointsOld, orbDescriptorsOld)
            trainList = orbKeyPointsOld.toList()
//            imgOld = img
        }

        val matches = MatOfDMatch()
        DescriptorMatcher.create(NORM_HAMMING).match(descriptors, orbDescriptorsOld, matches)
//        val matcher = BFMatcher(NORM_HAMMING, true)
//        BFMatcher(NORM_HAMMING, true).match(orbDescriptorsOld, descriptors, matches)
//        Features2d.drawMatches(img, keyPoints, img, orbKeyPointsOld, asdf, resultImg)
        Features2d.drawKeypoints(img, keyPoints, resultImg, Scalar(0.0, 255.0, 0.0))
        Features2d.drawKeypoints(resultImg, orbKeyPointsOld, resultImg, Scalar(255.0,0.0,0.0,127.0))

//        Log.d("OnFrame", "Matches:")
//        for(i in 0..matches.toArray().size) {
//
//        }
//        val asdf = intArrayOf(2)
//        val point1 = keyPoints[asdf]
//        Imgproc.line(resultImg, Point(0.0,0.0), point1 ,Scalar(0.0,0.0,255.0))

//        val list1 = mutableListOf<Point>()
//        val list2 = mutableListOf<Point>()
        val queryList = keyPoints.toList()
        val matchList = matches.toList()

        for(mat in matchList) {
//            list1.add(queryList[mat.queryIdx].pt)
//            list2.add(trainList[mat.trainIdx].pt)
            Imgproc.line(resultImg, queryList[mat.queryIdx].pt, trainList[mat.trainIdx].pt, Scalar(0.0,0.0,255.0))
        }

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
