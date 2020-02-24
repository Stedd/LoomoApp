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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.Loomo.LoomoBase
import com.example.loomoapp.Loomo.LoomoRealsense
import com.example.loomoapp.Loomo.LoomoSensor
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.*
import com.example.loomoapp.viewModel.MainActivityViewModel
import com.segway.robot.sdk.base.bind.ServiceBinder.BindStateListener
import com.segway.robot.sdk.emoji.module.head.Head
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.perception.sensor.Sensor
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.engine.OpenCVEngineInterface
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
const val TAG = "MainActivity"

lateinit var viewModel: MainActivityViewModel

//Initialize loomo SDK instances
lateinit var mLoomoBase: LoomoBase
lateinit var mLoomoRealsense: LoomoRealsense
lateinit var mLoomoSensor: LoomoSensor

//ROS classes
lateinit var mROSMain: ROSMain

val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread
var cameraRunning: Boolean = false

//OpenCV Variables
lateinit var mOpenCVMain: OpenCVMain


class MainActivity : AppCompatActivity() {

    private val mDistanceController = DistanceController()
    private var mControllerThread = Thread()

    //Import native functions
    external fun stringFromJNI(): String

    init {
        Log.d(TAG, "Init Main activity")
        //Load native
        System.loadLibrary("native-lib")

        mOpenCVMain.init()

        mROSMain.initMain()
    }

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")


        //Start Loomo SDK instances
        mLoomoBase      = LoomoBase()
        mLoomoRealsense = LoomoRealsense()
        mLoomoSensor    = LoomoSensor()
        mROSMain        = ROSMain()
        mOpenCVMain     = OpenCVMain()
        mOpenCVMain     .onCreate(this)

        //Initialize viewModel
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
//        viewModel = MainActivityViewModel()
//
//        viewModel.text.observe(this, Observer {
//            textView.text = it
//        })

        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))

//        viewModel.text.value = "Service not started"

        // Onclicklisteners
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

        mOpenCVMain.resume()

        //Bind Sensor SDK service
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


}
