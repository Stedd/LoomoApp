package com.example.loomoapp

import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.Loomo.*
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.ROSMain
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val UIThreadHandler = Handler() //Used to post messages to UI Thread (i.e. MainActivityViewModel)
    private lateinit var viewModel: MainActivityViewModel

    //Variables
    private val TAG = "MainActivity"

    //Initialize loomo classes
    lateinit var mLoomoBase: LoomoBase
    lateinit var mLoomoRealSense: LoomoRealSense
    lateinit var mLoomoSensor: LoomoSensor
//    lateinit var mLoomoControl: LoomoControl

    //ROS classes
    lateinit var mROSMain: ROSMain

    //OpenCV Variables
    lateinit var mOpenCVMain: OpenCVMain
    private lateinit var intentOpenCV: Intent

    //Import native functions
    external fun stringFromJNI(): String

    init {
        Log.d(TAG, "Init Main activity")

        //Load native
        System.loadLibrary("native-lib")
    }

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }
    private val camView by lazy {
        findViewById<ImageView>(R.id.camView)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")

        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        //Initialize classes
        mLoomoBase      = LoomoBase     (viewModel)
        mLoomoRealSense = LoomoRealSense(this, viewModel, UIThreadHandler)
        mLoomoRealSense.setActiveStateOfCameras(false, true, false)
        mLoomoSensor    = LoomoSensor   (viewModel)
//        mLoomoControl   = LoomoControl  (viewModel, mLoomoBase, mLoomoSensor)

        mROSMain        = ROSMain(mLoomoBase, mLoomoSensor, mLoomoRealSense)

        mOpenCVMain     = OpenCVMain()
        intentOpenCV    = Intent(this, mOpenCVMain::class.java)

        //Start OpenCV Service
        startService(intentOpenCV)

        //Start Ros Activity
        mROSMain.initMain()


//        mLoomoControl.mControllerThread.start()

        mOpenCVMain.onCreate(this, findViewById(R.id.javaCam))

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

//        viewModel.realSenseColorImage.observe(this, Observer {
//            camView.setImageBitmap(it)
//        })

        viewModel.imgFishEyeBitmap.observe(this, Observer {
            camView.setImageBitmap(it)
        })


        viewModel.text.value = "Service not started"

        // Onclicklisteners
        btnStartService.setOnClickListener {
//            mLoomoControl.startController(this,"ControllerThread start command")
        }
        btnStopService.setOnClickListener {
//            mLoomoControl.stopController(this,"ControllerThread stop command")
        }
        btnStartCamera.setOnClickListener {
            Log.d(TAG, "CamStartBtn clicked")
            mLoomoRealSense.startActiveCameras()
        }
        btnStopCamera.setOnClickListener {
            Log.d(TAG, "CamStopBtn clicked")
            mLoomoRealSense.stopActiveCameras()
        }

        //Helloworld from c++
        sample_text.text = stringFromJNI()
    }

    override fun onResume() {

        mOpenCVMain.resume()

        //Bind Sensor SDK service
        mLoomoSensor.bind(this)

        mLoomoRealSense.bind(this)

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
//        mLoomoControl.stopController(this,"App paused, Controller thread stopping")

//        mLoomoRealSense.stopCamera(this, "App paused, Camera thread stopping")

    }

}


