package com.example.loomoapp

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Pair
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.example.loomoapp.Loomo.LoomoBase
import com.example.loomoapp.Loomo.LoomoControl
import com.example.loomoapp.Loomo.LoomoRealsense
import com.example.loomoapp.Loomo.LoomoSensor
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.*
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.ros.address.InetAddressFactory
import org.ros.android.AppCompatRosActivity
import org.ros.android.RosActivity
import org.ros.message.Time
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import org.ros.time.NtpTimeProvider
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit


class MainActivity : RosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.31:11311/")) {

    private val UIThreadHandler = Handler() //Used to post messages to UI Thread

    //Variables
    private val TAG = "MainActivity"

    //Initialize loomo classes
    private lateinit var mLoomoBase: LoomoBase
    private lateinit var mLoomoRealsense: LoomoRealsense
    private lateinit var mLoomoSensor: LoomoSensor
    private lateinit var mLoomoControl: LoomoControl

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

//        init()
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
        Log.d(
            TAG,
            "sys: " + Time.fromMillis(System.currentTimeMillis())
        )
        ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.SECONDS)
        nodeConfiguration.timeProvider = ntpTimeProvider
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")

        //Initialize classes
        mLoomoBase = LoomoBase()
        mLoomoRealsense = LoomoRealsense()
        mLoomoSensor = LoomoSensor()
        mLoomoControl = LoomoControl(mLoomoBase, mLoomoSensor)

        //Publishers
        mRealsensePublisher = RealsensePublisher(mDepthStamps, mDepthRosStamps, mLoomoRealsense)
        mTFPublisher =
            TFPublisher(mDepthStamps, mDepthRosStamps, mLoomoBase, mLoomoSensor, mLoomoRealsense)
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

        mOpenCVMain = OpenCVMain()
        intentOpenCV = Intent(this, mOpenCVMain::class.java)

        //Start OpenCV Service
        startService(intentOpenCV)

        mLoomoControl.mControllerThread.start()

        mOpenCVMain.onCreate(this, findViewById(R.id.javaCam))

//        viewModel.text.observe(getLifecycleOwner(), Observer {
//            textView.text = it
//        })

//        viewModel.realSenseColorImage.observe(getLifecycleOwner(), Observer {
//            camView.setImageBitmap(it)
//        })


//        viewModel.text.value = "Service not started"

        // Onclicklisteners
        btnStartService.setOnClickListener {
            mLoomoControl.startController(this, "ControllerThread start command")
        }
        btnStopService.setOnClickListener {
            mLoomoControl.stopController(this, "ControllerThread stop command")
        }
        btnStartCamera.setOnClickListener {
            mLoomoRealsense.startCamera(this, "Camera start command")
        }
        btnStopCamera.setOnClickListener {
            mLoomoRealsense.stopCamera(this, "Camera stop command")
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

        mLoomoRealsense.bind(this)

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

        mLoomoRealsense.stopCamera(this, "App paused, Camera thread stopping")

    }

}


