package com.example.loomoapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.util.Pair
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.*
import com.example.loomoapp.Loomo.*
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
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
    AppCompatRosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.20:11311/")) {

//    private fun getLifecycleOwner(): LifecycleOwner {
//        var context: Context = this
//        while (context !is LifecycleOwner) {
//            context = (context as ContextWrapper).baseContext
//        }
//        return context
//    }

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


    //ROS classes
//    lateinit var mROSMain: ROSMain

    //OpenCV Variables
    private lateinit var mOpenCVMain: OpenCVMain
    private lateinit var intentOpenCV: Intent

    //Import native functions
    private external fun stringFromJNI(): String

    // Keep track of timestamps when images published, so corresponding TFs can be published too
    // Stores a co-ordinated platform time and ROS time to help manage the offset
    private val mDepthRosStamps: Queue<Pair<Long, Time>> = ConcurrentLinkedDeque<Pair<Long, Time>>()
    private val mDepthStamps: Queue<Long> = ConcurrentLinkedDeque<Long>()

    //Rosbridge
//    private lateinit var mOnNodeStarted: Runnable
//    private lateinit var mOnNodeShutdown: Runnable
    private lateinit var mBridgeNode: RosBridgeNode

    //Publishers
    private lateinit var mRealsensePublisher: RealsensePublisher
    private lateinit var mTFPublisher: TFPublisher
    private lateinit var mSensorPublisher: SensorPublisher
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

        //Initialize classes
        mLoomoBase = LoomoBase()
        mLoomoRealSense = LoomoRealSense()
        mLoomoSensor = LoomoSensor()
        mLoomoControl = LoomoControl(mLoomoBase, mLoomoSensor)

        //Publishers
        mRealsensePublisher = RealsensePublisher(mDepthStamps, mDepthRosStamps, mLoomoRealSense)
        mTFPublisher = TFPublisher(mDepthStamps, mDepthRosStamps, mLoomoBase, mLoomoSensor, mLoomoRealSense)
        mSensorPublisher = SensorPublisher(mLoomoSensor)
        mRosBridgeConsumers = listOf(mRealsensePublisher, mTFPublisher, mSensorPublisher)

        // TODO: 25/02/2020 Not sure what these are used for
//        mOnNodeStarted = Runnable {
//            // Node has started, so we can now tell publishers and subscribers that ROS has initialized
//            for (consumer in mRosBridgeConsumers) {
//                consumer.node_started(mBridgeNode)
//                // Try a call to start listening, this may fail if the Loomo SDK is not started yet (which is fine)
//                consumer.start()
//            }
//        }
//
//        // TODO: shutdown consumers correctly
//        mOnNodeShutdown = Runnable { }

        // Start an instance of the RosBridgeNode
        mBridgeNode = RosBridgeNode()

        //Start OpenCV Service
        mOpenCVMain = OpenCVMain()
        intentOpenCV = Intent(this, mOpenCVMain::class.java)
        startService(intentOpenCV)
        mOpenCVMain.onCreate(this, findViewById(R.id.javaCam))


        //Start Ros Activity
//        mROSMain.initMain()


        mLoomoControl.mControllerThread.start()

//        colorByteBuffer.observeForever { camViewColor.setImageBitmap(byteArrToBitmap(getByteArrFromByteBuf(it), 3, COLOR_WIDTH, COLOR_HEIGHT)) }
        colorByteBuffer.observeForever {
//            if (it != null) {
//                mOpenCVMain.newFrame(it)
//                camViewColor.setImageBitmap(mOpenCVMain.getFrame())
//            }
            val bmp = Bitmap.createBitmap(COLOR_WIDTH, COLOR_HEIGHT, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(it)
            camViewColor.setImageBitmap(bmp)
        }
        fishEyeByteBuffer.observeForever {
            if (it != null) {
                mOpenCVMain.newFrame(it)
                camViewFishEye.setImageBitmap(mOpenCVMain.getFrame())
            }
//            val bmp = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT, Bitmap.Config.ALPHA_8)
//            bmp.copyPixelsFromBuffer(it)
//            camViewFishEye.setImageBitmap(bmp)
        }
        depthByteBuffer.observeForever {
            val bmp = Bitmap.createBitmap(DEPTH_WIDTH, DEPTH_HEIGHT, Bitmap.Config.RGB_565)
            bmp.copyPixelsFromBuffer(it)
            camViewDepth.setImageBitmap(bmp)
        }

        camViewColor.visibility = ImageView.GONE
        camViewFishEye.visibility = ImageView.GONE
        camViewDepth.visibility = ImageView.GONE


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
            --camViewState
            camViewColor.visibility = ImageView.GONE
            camViewFishEye.visibility = ImageView.GONE
            camViewDepth.visibility = ImageView.GONE
        }
        btnStartService.setOnClickListener {
            Log.d(TAG, "ServStartBtn clicked")
            mLoomoControl.startController(this, "ControllerThread start command")
        }
        btnStopService.setOnClickListener {
            Log.d(TAG, "ServStopBtn clicked")
            mLoomoControl.stopController(this, "ControllerThread stop command")
        }

        //Helloworld from c++
        sample_text.text = stringFromJNI()
    }


    override fun onResume() {

        mOpenCVMain.resume()

        UIThreadHandler.postDelayed({
            for (consumer in mRosBridgeConsumers) {
                consumer.node_started(mBridgeNode)
                // Try a call to start listening, this may fail if the Loomo SDK is not started yet (which is fine)
                consumer.start()
            }
        }, 10000)

        mLoomoSensor.bind(this)

        mLoomoRealSense.bind(this)
        mLoomoRealSense.startColorCamera(UIThreadHandler, colorByteBuffer)
        mLoomoRealSense.startFishEyeCamera(UIThreadHandler, fishEyeByteBuffer)
        mLoomoRealSense.startDepthCamera(UIThreadHandler, depthByteBuffer)

        mLoomoBase.bind(this)

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


