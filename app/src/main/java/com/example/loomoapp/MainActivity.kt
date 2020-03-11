package com.example.loomoapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.*
import com.example.loomoapp.Loomo.*
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.*
import com.example.loomoapp.utils.copy
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ros.address.InetAddressFactory
import org.ros.android.AppCompatRosActivity
import org.ros.message.Time
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import org.ros.time.NtpTimeProvider
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
    lateinit var mLoomoBase: LoomoBase
    lateinit var mLoomoRealSense: LoomoRealSense
    lateinit var mLoomoSensor: LoomoSensor
    lateinit var mLoomoControl: LoomoControl

    private val fishEyeByteBuffer = MutableLiveData<ByteBuffer>()
    private val colorByteBuffer = MutableLiveData<ByteBuffer>()
    private val depthByteBuffer = MutableLiveData<ByteBuffer>()
    private val fishEyeBitmap = MutableLiveData<Bitmap>()
    private val colorBitmap = MutableLiveData<Bitmap>()
    private val depthBitmap = MutableLiveData<Bitmap>()
    private val fishEyeFrameInfo = MutableLiveData<FrameInfo>()
    private val colorFrameInfo = MutableLiveData<FrameInfo>()
    private val depthFrameInfo = MutableLiveData<FrameInfo>()

    val newFrame = MutableLiveData<Boolean>()

    //OpenCV Variables
    private lateinit var mOpenCVMain: OpenCVMain
//    private lateinit var mOpenCVMain: MainVisionService
    private lateinit var intentOpenCV: Intent

    //Import native functions
    private external fun stringFromJNI(): String

    // Keep track of timestamps when images published, so corresponding TFs can be published too
    // Stores a co-ordinated platform time and ROS time to help manage the offset
    private val mDepthRosStamps: Queue<Pair<Long, Time>> = ConcurrentLinkedDeque()
    private val mDepthStamps: Queue<Long> = ConcurrentLinkedDeque()

    //Rosbridge
    private lateinit var mBridgeNode: RosBridgeNode

    //Publishers
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
        Log.d(TAG, "sys: " + Time.fromMillis(System.currentTimeMillis()))
        ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES)
        nodeConfiguration.timeProvider = ntpTimeProvider
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")
        // Hacky trick to make the app fullscreen
        textView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        mRosPublisherThread =
            LoopedThread("ROS_Pub_Thread", Process.THREAD_PRIORITY_AUDIO)
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

        //Start OpenCV Service
        mOpenCVMain = OpenCVMain()
//        mOpenCVMain = MainVisionService()
        intentOpenCV = Intent(this, mOpenCVMain::class.java)
        startService(intentOpenCV)
        mOpenCVMain.onCreate(this)


        //Publishers
        mTFPublisher =
            TFPublisher(
                mDepthStamps,
                mDepthRosStamps,
                mLoomoBase,
                mLoomoSensor,
                mLoomoRealSense.mVision,
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



        //Start Ros Activity
//        mROSMain.initMain()


        mLoomoControl.mControllerThread.start()

//        val fishEyeThread = LoopedThread("FishEye observer", Process.THREAD_PRIORITY_DEFAULT)
//        val colorThread = LoopedThread("Color observer", Process.THREAD_PRIORITY_DEFAULT)
//        val depthThread = LoopedThread("Depth observer", Process.THREAD_PRIORITY_DEFAULT)
//        fishEyeThread.start()
//        colorThread.start()
//        depthThread.start()
//
//        depthByteBuffer.observeForever {
//            depthThread.handler.post {
//                val tic = System.currentTimeMillis()
//                mOpenCVMain.newDepthFrame(it.copy())
//                runOnUiThread {
//                    val ticc = System.currentTimeMillis()
//                    camViewDepth.setImageBitmap(mOpenCVMain.getDepthFrame())
//                    val tocc = System.currentTimeMillis()
//                    Log.d("Timing", "DepthUp: ${tocc-ticc}ms (${1000/(tocc-ticc)}FPS)")
//                }
//                val toc = System.currentTimeMillis()
//                Log.d("Timing", "Depth: ${toc-tic}ms (${1000/(toc-tic)}FPS)")
//            }
//        }
//        colorByteBuffer.observeForever {
//            colorThread.handler.post {
//                val tic = System.currentTimeMillis()
//                mOpenCVMain.newColorFrame(it.copy())
//                runOnUiThread {
//                    val ticc = System.currentTimeMillis()
//                    camViewColor.setImageBitmap(mOpenCVMain.getColorFrame())
//                    val tocc = System.currentTimeMillis()
//                    Log.d("Timing", "ColorUp: ${tocc-ticc}ms (${1000/(tocc-ticc)}FPS)")
//                }
//                val toc = System.currentTimeMillis()
//                Log.d("Timing", "Color: ${toc-tic}ms (${1000/(toc-tic)}FPS)")
//            }
//        }
//        var busy = false
//        fishEyeByteBuffer.observeForever {
//            if (!busy) {
//                busy = true
//                fishEyeThread.handler.post {
//                    val tic = System.currentTimeMillis()
//                    mOpenCVMain.newFishEyeFrame(it.copy())
//                    runOnUiThread {
//                        val ticc = System.currentTimeMillis()
//                        camViewFishEye.setImageBitmap(mOpenCVMain.getFishEyeFrame())
//                        val tocc = System.currentTimeMillis()
//                        Log.d("Timing", "FishEyeUp: ${tocc-ticc}ms (${1000/(tocc-ticc)}FPS)")
//                        busy = false
//                    }
//                    val toc = System.currentTimeMillis()
//                    Log.d("Timing", "FishEye: ${toc-tic}ms (${1000/(toc-tic)}FPS)")
//                }
//            }
//        }

//        fishEyeBitmap.observeForever { camViewFishEye.setImageBitmap(it) }
//        colorBitmap.observeForever { camViewColor.setImageBitmap(it) }
//        depthBitmap.observeForever { camViewDepth.setImageBitmap(it) }

        camViewColor.visibility = ImageView.GONE
        camViewFishEye.visibility = ImageView.VISIBLE
        camViewDepth.visibility = ImageView.GONE

        CoroutineScope(IO).launch {
            while(true) {
                mOpenCVMain.checkFrame(StreamType.FISH_EYE) {
                    CoroutineScope(Main).launch {
                        camViewFishEye.setImageBitmap(it)
                    }
                }
                mOpenCVMain.checkFrame(StreamType.COLOR) {
                    CoroutineScope(Main).launch {
                        camViewColor.setImageBitmap(it)
                    }
                }
                mOpenCVMain.checkFrame(StreamType.DEPTH) {
                    CoroutineScope(Main).launch {
                        camViewDepth.setImageBitmap(it)
                    }
                }

                delay(34)
            }
        }

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
                }
                2 -> {
                    camViewColor.visibility = ImageView.VISIBLE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.GONE
                }
                else -> {
                    camViewState = 0
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.VISIBLE
                    camViewDepth.visibility = ImageView.GONE
                }
            }
        }
        btnStopCamera.setOnClickListener {
            Log.d(TAG, "CamStopBtn clicked")
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
        mOpenCVMain.resume()

        mLoomoRealSense.bind(this)
        mLoomoRealSense.startCameras { streamType, frame -> mOpenCVMain.onNewFrame(streamType, frame) }
//        mLoomoRealSense.startColorCamera(UIThreadHandler, colorByteBuffer, colorFrameInfo)
//        mLoomoRealSense.startFishEyeCamera(UIThreadHandler, fishEyeByteBuffer, fishEyeFrameInfo)
//        mLoomoRealSense.startDepthCamera(UIThreadHandler, depthByteBuffer, depthFrameInfo)

        mLoomoSensor.bind(this)

        mLoomoBase.bind(this)


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


