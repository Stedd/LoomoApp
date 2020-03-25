package com.example.loomoapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.util.Pair
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.*
import com.example.loomoapp.Inference.Classifier
import com.example.loomoapp.Inference.InferenceMain
import com.example.loomoapp.Inference.TensorFlowYoloDetector
import com.example.loomoapp.Loomo.*
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.*
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ros.address.InetAddressFactory
import org.ros.android.AppCompatRosActivity
import org.ros.message.Time
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import org.ros.time.NtpTimeProvider
import org.tensorflow.Tensor
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


class MainActivity :
    AppCompatRosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.31:11311/")) {

    private val UIThreadHandler = Handler() //Used to post messages to UI Thread

    //Variables
    private val TAG = "MainActivity"

    //Declare loomo classes
    lateinit var mLoomoBase: LoomoBase
    lateinit var mLoomoRealSense: LoomoRealSense
    lateinit var mLoomoSensor: LoomoSensor
    lateinit var mLoomoControl: LoomoControl

    private val fishEyeByteBuffer = MutableLiveData<ByteBuffer>()
    private val colorByteBuffer = MutableLiveData<ByteBuffer>()
    private val depthByteBuffer = MutableLiveData<ByteBuffer>()
    private val fishEyeFrameInfo = MutableLiveData<FrameInfo>()
    private val colorFrameInfo = MutableLiveData<FrameInfo>()
    private val depthFrameInfo = MutableLiveData<FrameInfo>()
    private val inferenceImage = MutableLiveData<Bitmap>()

    //Inference Variables
    private lateinit var mInferenceThread: LoopedThread
    private lateinit var mInferenceMain : InferenceMain
    private lateinit var intentInference: Intent

    //OpenCV Variables
    private lateinit var mOpenCVMain: OpenCVMain
    private lateinit var intentOpenCV: Intent

    //Import native functions
    private external fun stringFromJNI(): String

    // Keep track of timestamps when images published, so corresponding TFs can be published too
    // Stores a co-ordinated platform time and ROS time to help manage the offset
    private val mDepthRosStamps: Queue<Pair<Long, Time>> = ConcurrentLinkedDeque()
    private val mDepthStamps: Queue<Long> = ConcurrentLinkedDeque()

    //Rosbridge
    private lateinit var mBridgeNode: RosBridgeNode

    //ROS Publishers
    private lateinit var mRosPublisherThread: LoopedThread
    private lateinit var mRealSensePublisher: RealsensePublisher
    private lateinit var mTFPublisher: TFPublisher
    private lateinit var mSensorPublisher: SensorPublisher
    private lateinit var mRosMainPublisher: RosMainPublisher
    private lateinit var mRosBridgeConsumers: List<RosBridge>

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    init {
        Log.d(TAG, "Init Main activity")

        //Load native
        System.loadLibrary("native-lib")
    }

    override fun init(nodeMainExecutor: NodeMainExecutor) {
        Log.d(TAG, "Init ROS node.")
        val nodeConfiguration = NodeConfiguration.newPublic(
            InetAddressFactory.newNonLoopback().hostAddress,
            masterUri
        )

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
        Log.d(TAG, "sys: " + Time.fromMillis(System.currentTimeMillis()))
        ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES)
        nodeConfiguration.timeProvider = ntpTimeProvider
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")

        mRosPublisherThread =
            LoopedThread("ROS_Pub_Thread", Process.THREAD_PRIORITY_DEFAULT)
        mRosPublisherThread.start()
        mRealSensePublisher =
            RealsensePublisher(
                mDepthStamps,
                mDepthRosStamps,
                mRosPublisherThread
            )

        //Initialize classes
        mLoomoBase = LoomoBase()
        mLoomoRealSense = LoomoRealSense(mRealSensePublisher)
        mLoomoSensor = LoomoSensor()
        mLoomoControl = LoomoControl(mLoomoBase, mLoomoSensor)

        //Publishers
        mTFPublisher =
            TFPublisher(
                mDepthStamps,
                mDepthRosStamps,
                mLoomoBase,
                mLoomoSensor,
                mLoomoRealSense,
                mRosPublisherThread
            )
        mSensorPublisher = SensorPublisher(mLoomoSensor, mRosPublisherThread)
        mRosBridgeConsumers = listOf(mRealSensePublisher, mTFPublisher, mSensorPublisher)

        mRosMainPublisher = RosMainPublisher(
            fishEyeByteBuffer,
            colorByteBuffer,
            depthByteBuffer,
            fishEyeFrameInfo,
            colorFrameInfo,
            depthFrameInfo,
            mRealSensePublisher,
            mSensorPublisher,
            mTFPublisher
        )


        // Start an instance of the RosBridgeNode
        mBridgeNode = RosBridgeNode()


        //Start Inference Service
        mInferenceThread    = LoopedThread("Inference_Thread", Process.THREAD_PRIORITY_DEFAULT)
        mInferenceThread    .start()
        mInferenceMain      = InferenceMain()
        intentInference     = Intent(this, mInferenceMain::class.java)
        startService(intentInference)
        mInferenceMain.setHandlerThread(mInferenceThread)
        mInferenceMain.setMainUIHandler(UIThreadHandler)
        mInferenceMain.setInferenceBitmap(inferenceImage)
        mInferenceMain.init(this)


        //Start OpenCV Service
        mOpenCVMain         = OpenCVMain()
        intentOpenCV        = Intent(this, mOpenCVMain::class.java)
        startService(intentOpenCV)
        mOpenCVMain.onCreate(this, findViewById(R.id.javaCam))


        mLoomoControl.mControllerThread.start()


        colorByteBuffer.observeForever {
//            if (it != null) {
//                mOpenCVMain.newFrame(it)
//                camViewColor.setImageBitmap(mOpenCVMain.getFrame())
//            }
            val bmp = Bitmap.createBitmap(COLOR_WIDTH, COLOR_HEIGHT, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(it.copy())
            mInferenceMain.newFrame(bmp)
            camViewColor.setImageBitmap(bmp)
        }

        fishEyeByteBuffer.observeForever {
            if (it != null) {
                mOpenCVMain.newFrame(it.copy())
//                mInferenceMain.newFrame(it.toByteArray(), mOpenCVMain.getFrame()) //// TODO: 24.03.2020 neural net expects colors currently
                camViewFishEye.setImageBitmap(mOpenCVMain.getFrame())// TODO: 23.03.2020 Add overlay from inference, toggleable?
//                camViewFishEye.setImageBitmap(mInferenceMain.getFrame())// TODO: 24.03.2020 neural net expects colors currently
            }
//            val bmp = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT, Bitmap.Config.ALPHA_8)
//            bmp.copyPixelsFromBuffer(it)
//            camViewFishEye.setImageBitmap(bmp)
        }

        depthByteBuffer.observeForever {
            val bmp = Bitmap.createBitmap(DEPTH_WIDTH, DEPTH_HEIGHT, Bitmap.Config.RGB_565)
            bmp.copyPixelsFromBuffer(it.copy())
            camViewDepth.setImageBitmap(bmp)
        }

        inferenceImage.observeForever {
            inferenceView.setImageBitmap(it)
        }


        camViewColor.visibility = ImageView.GONE
        camViewFishEye.visibility = ImageView.GONE
        camViewDepth.visibility = ImageView.GONE
        inferenceView.visibility = ImageView.GONE


        // Onclicklisteners
        var camViewState = 0
        btnStartCamera.setOnClickListener {
            Log.d(TAG, "CamStartBtn clicked")
            ++camViewState
            when (camViewState) {
                1 -> {
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.VISIBLE
                    inferenceView.visibility = ImageView.GONE
                }
                2 -> {
                    camViewColor.visibility = ImageView.VISIBLE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.GONE
                    inferenceView.visibility = ImageView.GONE
                }
                3 -> {
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.VISIBLE
                    camViewDepth.visibility = ImageView.GONE
                    inferenceView.visibility = ImageView.GONE
                }
                else -> {
                    camViewState = 0
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.GONE
                    inferenceView.visibility = ImageView.VISIBLE
                }
            }
        }
        btnStopCamera.setOnClickListener {
            Log.d(TAG, "CamStopBtn clicked")
            --camViewState
            camViewColor.visibility = ImageView.GONE
            camViewFishEye.visibility = ImageView.GONE
            camViewDepth.visibility = ImageView.GONE
        }
        btnStartService.setOnClickListener {
            Log.d(TAG, "ServStartBtn clicked")
            mRosMainPublisher.publishAllCameras()

        }
        btnStopService.setOnClickListener {
            Log.d(TAG, "ServStopBtn clicked")
            mRosMainPublisher.publishGraph()
        }

        //Helloworld from c++
        sample_text.text = stringFromJNI()
    }


    override fun onResume() {
        mLoomoRealSense.bind(this)
        mLoomoRealSense.startColorCamera(UIThreadHandler, colorByteBuffer, colorFrameInfo)
        mLoomoRealSense.startFishEyeCamera(UIThreadHandler, fishEyeByteBuffer, fishEyeFrameInfo)
        mLoomoRealSense.startDepthCamera(UIThreadHandler, depthByteBuffer, depthFrameInfo)

        mLoomoSensor.bind(this)

        mLoomoBase.bind(this)

        mOpenCVMain.resume()

        UIThreadHandler.postDelayed({
            mRealSensePublisher.node_started(mBridgeNode)
            mTFPublisher.node_started(mBridgeNode)
            mSensorPublisher.node_started(mBridgeNode)
        }, 10000)

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
        mLoomoControl.stopController(this, "App paused, Controller thread stopping")
    }
}


