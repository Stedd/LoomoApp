package com.example.loomoapp

import android.content.Intent
import android.os.*
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.*
import com.example.loomoapp.Loomo.*
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.OpenCV.VisualOdometry
import com.example.loomoapp.OpenCV.toBitmap
import com.example.loomoapp.ROS.*
import com.example.loomoapp.utils.LoopedThread
import com.example.loomoapp.utils.NonBlockingInfLoop
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import org.ros.address.InetAddressFactory
import org.ros.android.AppCompatRosActivity
import org.ros.message.Time
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import org.ros.time.NtpTimeProvider
import java.lang.NullPointerException
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit


class MainActivity :
    AppCompatRosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.31:11311/")) {

    private val UIThreadHandler = Handler() //Used to post messages to UI Thread

    //Variables
    private val TAG = "MainActivity"

    //Declare loomo classes
    lateinit var mLoomoControl: LoomoControl

    //TODO: Fix LoomoRealSense or OpenCVMain so that ROS publisher gets these vals.
    // These vals are used by ROS publisher, but nothing is assigned to them.
    // Can probably be fixed by adding a function in the lambda expression in:
    // com/example/loomoapp/MainActivity.kt:247
    private val fishEyeByteBuffer = MutableLiveData<ByteBuffer>()
    private val colorByteBuffer = MutableLiveData<ByteBuffer>()
    private val depthByteBuffer = MutableLiveData<ByteBuffer>()
    private val fishEyeFrameInfo = MutableLiveData<FrameInfo>()
    private val colorFrameInfo = MutableLiveData<FrameInfo>()
    private val depthFrameInfo = MutableLiveData<FrameInfo>()


    //OpenCV Variables
    private lateinit var mOpenCVMain: OpenCVMain
    private lateinit var intentOpenCV: Intent

    //Import native functions
//    private external fun stringFromJNI(): String

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
//        System.loadLibrary("native-lib")
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
        // Hacky trick to make the app fullscreen:
        textView1.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

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
        mLoomoControl = LoomoControl(LoomoBase, LoomoSensor)

        //Start OpenCV Service
        mOpenCVMain = OpenCVMain()
        intentOpenCV = Intent(this, mOpenCVMain::class.java)
        startService(intentOpenCV)
        mOpenCVMain.onCreate(this)


        //Publishers
        mTFPublisher =
            TFPublisher(
                mDepthStamps,
                mDepthRosStamps,
                LoomoBase,
                LoomoSensor,
//                mLoomoRealSense.mVision,
                LoomoRealSense.mVision,
                mRosPublisherThread
            )
        mSensorPublisher = SensorPublisher(LoomoSensor, mRosPublisherThread)
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

        //Start Ros Activity
//        mROSMain.initMain()

        mLoomoControl.mControllerThread.start()

        camViewColor.visibility = ImageView.GONE
        camViewFishEye.visibility = ImageView.VISIBLE
        camViewDepth.visibility = ImageView.GONE
        trajView.visibility = ImageView.GONE
        inferenceView.visibility = ImageView.GONE

        // Onclicklisteners
        var camViewState = 0
        btnStartCamera.setOnClickListener {
//            Log.d(TAG, "CamStartBtn clicked")
            ++camViewState
            when (camViewState) {
                1 -> {
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.VISIBLE
                    trajView.visibility = ImageView.GONE
                }
                2 -> {
                    camViewColor.visibility = ImageView.VISIBLE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.GONE
                    trajView.visibility = ImageView.GONE
                }
                3 -> {
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.GONE
                    trajView.visibility = ImageView.VISIBLE
                }
                else -> {
                    camViewState = 0
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.VISIBLE
                    camViewDepth.visibility = ImageView.GONE
                    trajView.visibility = ImageView.GONE
                }
            }
        }
        btnStopCamera.setOnClickListener {
//            Log.d(TAG, "CamStopBtn clicked")
            camViewColor.visibility = ImageView.GONE
            camViewFishEye.visibility = ImageView.GONE
            camViewDepth.visibility = ImageView.GONE
        }
        btnStartService.setOnClickListener {
//            Log.d(TAG, "ServStartBtn clicked")
            mOpenCVMain.toggle = !mOpenCVMain.toggle
//            mRosMainPublisher.publishAllCameras()
        }
        btnStopService.setOnClickListener {
            Log.d(TAG, "ServStopBtn clicked")
//            mRosMainPublisher.publishGraph()
        }

        //Helloworld from c++
//        sample_text.text = stringFromJNI()
    }



    override fun onResume() {
        mOpenCVMain.resume()

//        mLoomoRealSense.bind(this)
        LoomoRealSense.bind(this, mRealSensePublisher)
//        mLoomoRealSense.startCameras { streamType, frame ->
        LoomoRealSense.startCameras { streamType, frame ->
            mOpenCVMain.onNewFrame(streamType, frame)
            updateImgViews()
        }


        LoomoSensor.bind(this)

        LoomoBase.bind(this)

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

    private fun updateImgViews() {
        mOpenCVMain.getNewestFrame(StreamType.FISH_EYE) {
            runOnUiThread {camViewFishEye.setImageBitmap(it)}
        }
        mOpenCVMain.getNewestFrame(StreamType.COLOR) {
            runOnUiThread {camViewColor.setImageBitmap(it)}
//            mInferenceMain.newFrame(it)
        }
        mOpenCVMain.getNewestFrame(StreamType.DEPTH) {
            runOnUiThread {camViewDepth.setImageBitmap(it)}
        }
        runOnUiThread { trajView.setImageBitmap(mOpenCVMain.map.toBitmap()) }

        runOnUiThread {sample_text.text = "Features: ${mOpenCVMain.fishEyeTracker.numOfFeatures}, Expected n.o. feats: ${mOpenCVMain.fishEyeTracker.expectedNumOfFeatures}"}
        runOnUiThread {
            try {
                textView1.text = "Current position: ${VisualOdometry.posHistory.peek().dump()}"
            } catch (e: NullPointerException) {
                textView1.text = "null"
            }
        }
    }
}


