package com.example.loomoapp

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.Loomo.LoomoBase
import com.example.loomoapp.Loomo.LoomoControl
import com.example.loomoapp.Loomo.LoomoRealsense
import com.example.loomoapp.Loomo.LoomoSensor
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.ROSMain
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var UIThreadHandler = Handler() //Used to post messages to UI Thread
    val viewModel: MainActivityViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

    //Variables
    private val TAG = "MainActivity"

    //Initialize loomo classes
    val mLoomoBase: LoomoBase = LoomoBase(viewModel)
    val mLoomoRealsense: LoomoRealsense = LoomoRealsense(viewModel)
    val mLoomoSensor: LoomoSensor = LoomoSensor(viewModel)
    val mLoomoControl: LoomoControl= LoomoControl(viewModel, mLoomoBase, mLoomoSensor)



    //ROS classes
    private val mROSMain: ROSMain = ROSMain()

    //OpenCV Variables
    private val mOpenCVMain: OpenCVMain = OpenCVMain()
    private val intentOpenCV = Intent(this, mOpenCVMain::class.java)

    //Import native functions
    external fun stringFromJNI(): String

    init {
        Log.d(TAG, "Init Main activity")

        //Initialize viewModel
//        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        //Load native
        System.loadLibrary("native-lib")

        //Start OpenCV Service
        startService(intentOpenCV)

        //Start Ros Activity
        mROSMain.initMain()
    }

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")

        mLoomoControl.mControllerThread.start()

        mOpenCVMain.onCreate(this, findViewById(R.id.javaCam))

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

        viewModel.realSenseColorImage.observe(this, Observer {
            camView.setImageBitmap(it)
        })


        viewModel.text.value = "Service not started"

        // Onclicklisteners
        btnStartService.setOnClickListener {
            mLoomoControl.startController(this,"ControllerThread start command")
        }
        btnStopService.setOnClickListener {
            mLoomoControl.stopController(this,"ControllerThread stop command")
        }
        btnStartCamera.setOnClickListener {
            mLoomoRealsense.startCamera(this, "Camera start command")
        }
        btnStopCamera.setOnClickListener {
            mLoomoRealsense.stopCamera(this,"Camera stop command")
        }

        //Helloworld from c++
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
        mLoomoControl.stopController(this,"App paused, Controller thread stopping")

        mLoomoRealsense.stopCamera(this, "App paused, Camera thread stopping")

    }

}


