package com.example.loomoapp

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import com.example.loomoapp.ROS.*
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.base.bind.ServiceBinder.BindStateListener
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.Features2d
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.ros.address.InetAddressFactory
import org.ros.android.RosActivity
import org.ros.message.Time
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import org.ros.time.NtpTimeProvider
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit


//Variables
const val TAG = "debugMSG"

//lateinit var viewModel: MainActivityViewModel
lateinit var mBase: Base
lateinit var mVision: Vision
lateinit var mLoomoSensor: LoomoSensor
lateinit var mLoaderCallback: BaseLoaderCallback

lateinit var mRealsensePublisher: RealsensePublisher
lateinit var mTFPublisher: TFPublisher
lateinit var mSensorPublisher: SensorPublisher
lateinit var mRosBridgeConsumers: List<RosBridge>
lateinit var mBridgeNode: RosBridgeNode

lateinit var mDepthRosStamps // Stores a co-ordinated platform time and ROS time to help manage the offset
        : Queue<Pair<Long, Time>>

lateinit var mDepthStamps: Queue<Long>


val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread
var cameraRunning: Boolean = false

var img = Mat()
var imgFisheye = Mat()
var resultImg = Mat()
var resultImgFisheye = Mat()


class MainActivity : RosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.31:11311/")),
    CameraBridgeViewBase.CvCameraViewListener2 {

    private val mDistanceController = DistanceController()
    private var mControllerThread = Thread()




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


        //ROS
        // Keep track of timestamps when images published, so corresponding TFs can be published too
        mDepthStamps = ConcurrentLinkedDeque<Long>()
        mDepthRosStamps = ConcurrentLinkedDeque<Pair<Long, Time>>()
        mRealsensePublisher = RealsensePublisher(mDepthStamps, mDepthRosStamps)
        mTFPublisher = TFPublisher(mDepthStamps, mDepthRosStamps)
        mSensorPublisher = SensorPublisher()

        mRosBridgeConsumers = listOf(
            mRealsensePublisher,
            mTFPublisher,
            mSensorPublisher
        )
        val mOnNodeStarted = Runnable {
            // Node has started, so we can now tell publishers and subscribers that ROS has initialized
            for (consumer in mRosBridgeConsumers) {
                consumer.node_started(mBridgeNode)
                // Try a call to start listening, this may fail if the Loomo SDK is not started yet (which is fine)
                consumer.start()
            }
            // Special nodes
// TODO: we should handle this in the generic "start()" function
            mRealsensePublisher.start_color()
            mRealsensePublisher.start_depth()
        }

        // TODO: shutdown consumers correctly
        val mOnNodeShutdown = Runnable { }

        // Start an instance of the LoomoRosBridgeNode
        mBridgeNode = RosBridgeNode(mOnNodeStarted, mOnNodeShutdown)

    }

    override fun init(nodeMainExecutor: NodeMainExecutor) {
        Log.d(TAG, "init().")
        val nodeConfiguration = NodeConfiguration.newPublic(
            InetAddressFactory.newNonLoopback().hostAddress,
            masterUri
        )
        // Note: NTPd on Linux will, by default, not allow NTP queries from the local networks.
        // Add a rule like this to /etc/ntp.conf:
        //
        // restrict 192.168.86.0 mask 255.255.255.0 nomodify notrap nopeer
        //
        // Where the IP address is based on your subnet
        val ntpTimeProvider = NtpTimeProvider(
            InetAddressFactory.newFromHostString(masterUri.host),
            nodeMainExecutor.scheduledExecutorService
        )
        try {
            ntpTimeProvider.updateTime()
        } catch (e: Exception) {
            Log.d(TAG, "exception when updating time...")
        }
        Log.d(TAG, "master uri: " + masterUri.host)
        Log.d(TAG, "ros: " + ntpTimeProvider.currentTime.toString())
        Log.d(
            TAG,
            "sys: " + Time.fromMillis(System.currentTimeMillis())
        )
        ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES)
        nodeConfiguration.timeProvider = ntpTimeProvider
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration)
    }


    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    private val imgWidthColor = 640
    private val imgHeightColor = 480
    private var mImgColor = Bitmap.createBitmap(
        imgWidthColor,
        imgHeightColor,
        Bitmap.Config.ARGB_8888
    )
    private var mImgColorResult = Bitmap.createBitmap(
        imgWidthColor,
        imgHeightColor,
        Bitmap.Config.ARGB_8888
    )

    private val imgWidthFishEye = 640
    private val imgHeightFishEye = 480
    private var mImgFishEye = Bitmap.createBitmap(
        imgWidthFishEye,
        imgHeightFishEye,
        Bitmap.Config.ALPHA_8
    )
    private var mImgFishEyeResult = Bitmap.createBitmap(
        imgWidthFishEye,
        imgHeightFishEye,
        Bitmap.Config.ARGB_8888
    )

    private val imgWidthDepth = 320
    private val imgHeightDepth = 240
    private var mImgDepth = Bitmap.createBitmap(
        imgWidthDepth,
        imgHeightDepth,
        Bitmap.Config.RGB_565
    )
    private var mImgDepthResult = Bitmap.createBitmap(
        imgWidthDepth,
        imgHeightDepth,
        Bitmap.Config.ARGB_8888
    )
    //    private var mImgDepthCanny = Bitmap.createBitmap(
//        imgWidth/3,
//        imgHeight/3,
//        Bitmap.Config.RGB_565
//    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels
    private var mImgDepthScaled =
        Bitmap.createScaledBitmap(mImgDepth, imgWidthColor / 3, imgWidthColor / 3, false)

    private val detectorColorCam: ORB = ORB.create(10, 1.9F)
    private val keypointsColorCam = MatOfKeyPoint()
    private val detectorAndroidCam: ORB = ORB.create(10, 1.9F)
    private val keypointsAndroidCam = MatOfKeyPoint()


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
//        mVision = Vision.getInstance()
        mLoomoSensor = LoomoSensor(this)

////        viewModel = ViewModelProvider().get(MainActivityViewModel::class.java)
//        viewModel = MainActivityViewModel()
//
//        viewModel.text.observe(this, Observer {
//            textView.text = it
//        })





        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))

//        viewModel.text.value = "Service not started"

        btnStartService.setOnClickListener {
            startController("ControllerThread start command")
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

        // get Vision SDK instance
        mVision = Vision.getInstance()
        mVision.bindService(this, mBindVisionListener)

        // get Sensor SDK instance
//        mSensor = Sensor.getInstance()

        // get Locomotion SDK instance
//        mBase = Base.getInstance()

        // get Head SDK instance
//        mHead = Head.getInstance()



        //Bind loomo services
        mBase.bindService(this.applicationContext, object : BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Base onBind")
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Base unBind. Reason: $reason")
            }
        })
//
//        mVision.bindService(this.applicationContext, object : ServiceBinder.BindStateListener {
//            override fun onBind() {
//                Log.d(TAG, "Vision onBind")
//            }
//
//            override fun onUnbind(reason: String?) {
//                Log.d(TAG, "Vision unBind. Reason $reason")
//            }
//        })

        super.onResume()
    }




    private var mBindVisionListener: BindStateListener = object : BindStateListener {
        override fun onBind() {
            if (mVision.isBind) {
                Log.i(TAG, "onBind() mBindVisionListener called")
                mRealsensePublisher.loomo_started(mVision)
                mTFPublisher.loomo_started(mVision)
                Log.d(TAG, "bindVision enabling realsense switches.")

                mTFPublisher.start()
//                mRealsensePublisher.start_color()
////                mRealsensePublisher.start_depth()
////                mRealsensePublisher.start_fisheye()
            }
        }

        override fun onUnbind(reason: String) {
            Log.i(TAG, "onUnbindVision: $reason")
        }
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
                mVision.startListenFrame(StreamType.COLOR) { streamType, frame ->
                    mImgColor.copyPixelsFromBuffer(frame.byteBuffer)
//                    mImgColor = mImgFishEye.copy(Bitmap.Config.ARGB_8888, true)

                    //Convert to Mat
                    Utils.bitmapToMat(mImgColor, imgFisheye)

                    //Canny edge detector
                    Imgproc.Canny(imgFisheye, resultImgFisheye, 0.01, 190.0)

                    //ORB feature detector
//                    detectorColorCam.detect(imgFisheye, keypointsColorCam)
//                    Features2d.drawKeypoints(imgFisheye, keypointsColorCam, resultImgFisheye)

                    //Convert to Bitmap
                    Utils.matToBitmap(resultImgFisheye, mImgColorResult)

                    //Scale result image
//                    mImgDepthScaled = Bitmap.createScaledBitmap(mImgColorResult, imgWidthColor/3, imgWidthColor/3,false)

                    //Show result image on main ui
                    threadHandler.post {
                        camView.setImageBitmap(mImgColorResult.copy(Bitmap.Config.ARGB_8888, true))
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
            mVision.stopListenFrame(StreamType.COLOR)
            cameraRunning = false
        }
        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//        Log.i("cam", "new frame")
        img = inputFrame.gray()
//        resultImg = img
//        Imgproc.blur(img, resultImg, Size(15.0, 15.0))
//        Imgproc.GaussianBlur(img, resultImg, Size(15.0, 15.0), 1.0)
//        Imgproc.Canny(img, resultImg, 0.01, 190.0)

        detectorAndroidCam.detect(img, keypointsAndroidCam)

//        Log.i("cam", "Keypoints:$keypointsAndroidCam")

        Features2d.drawKeypoints(img, keypointsAndroidCam, resultImg)

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
