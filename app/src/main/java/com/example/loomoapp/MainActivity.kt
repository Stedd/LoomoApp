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
import org.opencv.core.*
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video


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
        imgWidth / 3,
        imgHeight / 3,
        Bitmap.Config.ARGB_8888
    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels

    private var mImgDepthScaled =
        Bitmap.createScaledBitmap(mImgDepth, imgWidth / 3, imgHeight / 3, false)


    private val mDistanceController = DistanceController()
    private var mControllerThread = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
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
            stopCamera("Camera stop command")
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
                    mImgDepthScaled =
                        Bitmap.createScaledBitmap(mImgDepth, imgWidth / 3, imgHeight / 3, false)
                    Utils.bitmapToMat(mImgDepthScaled, imgFisheye)
                    Imgproc.Canny(imgFisheye, resultImgFisheye, 0.01, 190.0)
                    Utils.matToBitmap(resultImgFisheye, mImgDepthCanny)

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

    private val detector: ORB = ORB.create()
    private var keyPoints = MatOfKeyPoint()
    private val descriptors = Mat()
    private var points = MatOfPoint2f()
    private var prevPoints = MatOfPoint2f()
    private var pointsOG = MatOfPoint2f()
    private var status = MatOfByte()
    private var totalReceivedFrames = 0

    private var expectedNumOfFeatures = 0
    private val MIN_NUM_OF_FEATURES = 20
    private var minNumOfFeatures = 20
    private var numOfFeatures = 0

    private val prevImg = Mat()

    private var pointPair = Pair<MatOfPoint2f, MatOfPoint2f>(prevPoints, points)


    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        img = inputFrame.rgba()

        totalReceivedFrames++

        if ((numOfFeatures < minNumOfFeatures) or captureNewKeyFrame) {
            captureNewKeyFrame = false
            detect(img)
            Log.d(
                TAG,
                "numOfFeatures: $numOfFeatures, minNumOfFeatures: $minNumOfFeatures, expectedNumOfFeatures: $expectedNumOfFeatures"
            )
        }

        trackFeatures(img)
        img.copyTo(prevImg)

        val ptArr = points.toArray()
//        val prevPtArr = pointsOld.toArray()
        val prevPtArr = pointsOG.toArray()
        if (ptArr.isNotEmpty() and (ptArr.size <= prevPtArr.size)) {
            for (index in ptArr.indices) {
                Imgproc.line(
                    img,
                    prevPtArr[index],
                    ptArr[index],
                    Scalar(0.0, 0.0, 255.0, 30.0)
                )
                Imgproc.circle(
                    img,
                    prevPtArr[index],
                    3,
                    Scalar(0.0, 255.0, 0.0, 127.0)
                )
                Imgproc.circle(
                    img,
                    ptArr[index],
                    3,
                    Scalar(255.0, 0.0, 255.0, 127.0)
                )
            }
        }

        runOnUiThread { viewModel.text.value = "Feats: $numOfFeatures, expected feats: $expectedNumOfFeatures" }
        return img
    }

    private val alpha = 0.2
    private fun detect(img: Mat) {
        detector.detect(img, keyPoints)
        detector.compute(img, keyPoints, descriptors)
//        keyPoints.convertTo(points, CV_32F)
        val kpTmp = keyPoints.toArray()
        val p2fTmp = Array<Point>(kpTmp.size) { kpTmp[it].pt }
        points = MatOfPoint2f(*p2fTmp)
        points.copyTo(pointsOG)
        points.copyTo(prevPoints)
        numOfFeatures = kpTmp.size
        expectedNumOfFeatures = ((1 - alpha) * expectedNumOfFeatures + alpha * numOfFeatures).toInt()
        minNumOfFeatures = if ((0.5 * expectedNumOfFeatures).toInt() > MIN_NUM_OF_FEATURES) {
            (0.5 * expectedNumOfFeatures).toInt()
        } else {
            MIN_NUM_OF_FEATURES
        }
    }

    private fun trackFeatures(img: Mat) {
        val err = MatOfFloat()
        val winSize = Size(21.0, 21.0)
        val termCrit = TermCriteria(TermCriteria.COUNT or TermCriteria.EPS, 30, 0.1)

        if ((prevImg.empty()) or (prevPoints.size().area() <= 0)) {
            Log.d(
                TAG,
                "prevImg empty: ${prevImg.empty()}, or pointsOld size: ${prevPoints.size().area()}"
            )
            img.copyTo(prevImg)
            points.copyTo(prevPoints)
            return
        }


        try {
            Video.calcOpticalFlowPyrLK(
                prevImg,
                img,
                prevPoints,
                points,
                status,
                err,
                winSize,
                3,
                termCrit,
                0,
                0.001
            )
        } catch (e: Exception) {
            Log.d(TAG, "Something wrong with Video.calcOpticalFlowPyrLK")
            Thread.sleep(500)
            points.copyTo(prevPoints)
        }

        // Remove points where tracking failed, or where they have gone outside the frame
        val statusList = status.toList()
        val pointsOldList = prevPoints.toList()
        val pointsList = points.toList()
//        val tmpStatus = mutableListOf<Byte>()
        val tmpPointsOld = mutableListOf<Point>()
        val tmpPoints = mutableListOf<Point>()
        var indexCorrection = 0
        val pointsOGList = pointsOG.toList()
        val tmpOGlist = mutableListOf<Point>()
        for ((index, stat) in statusList.withIndex()) {
            val pt = pointsOldList[index - indexCorrection]
            if ((stat.toInt() == 0) or (pt.x < 0) or (pt.y < 0)) {
                if ((pt.x < 0) or (pt.y < 0)) {
                    statusList[index] = 0.toByte()
                }
                indexCorrection++
            } else {
//                tmpStatus.add(stat)
                tmpPointsOld.add(pointsOldList[index])
                tmpPoints.add(pointsList[index])
                tmpOGlist.add(pointsOGList[index])
            }
        }
        if (pointsList.size != tmpPoints.size) {
            numOfFeatures = tmpPoints.size
            status = MatOfByte(*statusList.toByteArray())
//            status = MatOfByte(*tmpStatus.toByteArray())
            points = MatOfPoint2f(*tmpPoints.toTypedArray())
            prevPoints = MatOfPoint2f(*tmpPointsOld.toTypedArray())
            pointsOG = MatOfPoint2f(*tmpOGlist.toTypedArray())
            pointPair = Pair(prevPoints, points)
//            keyPoints = MatOfKeyPoint(*Array<KeyPoint>(numOfFeatures) {
//                KeyPoint(tmpPoints[it].x.toFloat(), tmpPoints[it].y.toFloat(), 1F)
//            })
        }
        points.copyTo(prevPoints)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.i("cam", "camera view started. width:$width, height: $height")
        img = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        Log.i("cam", "camera view stopped")
    }
}
