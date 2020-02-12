package com.example.loomoapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.vision.Vision
import kotlinx.android.synthetic.main.activity_main.*

//Variables
lateinit var viewModel: MainActivityViewModel
const val TAG = "debugMSG"
lateinit var mBase: Base
lateinit var mVision: Vision
lateinit var mLoomoSensor: LoomoSensor
val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread


class MainActivity : AppCompatActivity() {

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    private val mDistanceController = DistanceController()

    private val mCamera = object : ThreadLoop() {

        override val interval: Long = 500
        override var enable = true

        var asd = 0

        override fun main() {
            //some logic
            asd++
            //post to viewModel
            threadHandler.post {
                viewModel.text.value = "Value: $asd"
            }
        }

        override fun close() {
            asd = 0
            threadHandler.post {
                viewModel.text.value = "Thread Stopped"
            }
        }
    }

//    private var mControllerThread = Thread(mDistanceController, "ControllerThread")
    private var mControllerThread = Thread()
    private var mVisionThread = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")

        mBase = Base.getInstance()
        mVision = Vision.getInstance()
        mLoomoSensor = LoomoSensor(this)

        viewModel = ViewModelProvider(this)
            .get(MainActivityViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

        viewModel.text.value = "Service not started"

        if (mControllerThread.isAlive) {
            Log.i(TAG, "Thread started")
            viewModel.text.value = "Thread started"
        } else {
            Log.i(TAG, "Thread not started")
            viewModel.text.value = "Thread not started"
        }

        btnStartService.setOnClickListener {
            startService()
        }
        btnStopService.setOnClickListener {
            stopService()
        }
        btnStartCamera.setOnClickListener {
            startCamera()
        }
        btnStopCamera.setOnClickListener {
            stopCamera()
        }
    }



    override fun onResume() {
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
        if (mControllerThread.isAlive) {
            Log.i(TAG, "App destroyed, Thread stopping")
            mDistanceController.enable = false
        }
        super.onDestroy()
    }


    override fun onPause() {
        if (mControllerThread.isAlive) {
            Log.i(TAG, "App paused, Thread stopping")
            mDistanceController.enable = false
        }
        super.onPause()
    }

    private fun startService() {
        if (!mControllerThread.isAlive) {
            Log.i(TAG, "ControllerThread starting")
            mDistanceController.enable = true
            mControllerThread = Thread(mDistanceController, "ControllerThread")
            mControllerThread.start()
        }
    }

    private fun stopService() {
        Log.i(TAG, "Service stop command")
        mDistanceController.enable = false
    }

    private fun startCamera() {
        if (mVision.isBind) {
            Log.i(TAG, "Camera Thread starting")
            mCamera.enable = true
            mVisionThread = Thread(mCamera, "CameraThread")
            mVisionThread.start()
        }else{
            Toast.makeText(this, "Vision service not started yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera() {
        mCamera.enable = false
    }



}
