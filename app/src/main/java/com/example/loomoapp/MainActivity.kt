package com.example.loomoapp

import android.graphics.Bitmap
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
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*

//Variables
lateinit var viewModel: MainActivityViewModel
const val TAG = "debugMSG"
lateinit var mBase: Base
lateinit var mVision: Vision
lateinit var mLoomoSensor: LoomoSensor
val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread
var cameraRunning: Boolean = false

class MainActivity : AppCompatActivity() {

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    private val mDistanceController = DistanceController()


    private val imgWidth = 320
    private val imgHeight = 240
    private var mImgDepth = Bitmap.createBitmap(
        imgWidth,
        imgHeight,
        Bitmap.Config.RGB_565
    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels


    private val mCamera = object : ThreadLoop() {

        override var interval: Long = 10
        override var enable = true

        override fun main() {
            //some logic

            //post to viewModel
            threadHandler.post {
                //                viewModel.image.setImageBitmap(mImgDepth.copy(Bitmap.Config.ARGB_8888, true))
                camView.setImageBitmap(mImgDepth.copy(Bitmap.Config.ARGB_8888, true))
            }
        }

        override fun close() {
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

//        if (mControllerThread.isAlive) {
//            Log.i(TAG, "Thread started")
//            viewModel.text.value = "Thread started"
//        } else {
//            Log.i(TAG, "Thread not started")
//            viewModel.text.value = "Thread not started"
//        }

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
            Log.i(TAG, "App paused, Controller thread stopping")
//            mDistanceController.enable = false
            stopService()
        }
        if (mVision.isBind) {
            Log.i(TAG, "App paused, Camera thread stopping")
//            mCamera.enable = false
//            mVision.stopListenFrame(StreamType.DEPTH)
            stopCamera()
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
            if (!cameraRunning) {
                mVision.startListenFrame(StreamType.DEPTH) { streamType, frame ->
                    mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
                    threadHandler.post {
                        camView.setImageBitmap(mImgDepth.copy(Bitmap.Config.ARGB_8888, true))
                    }
                }
                cameraRunning = true
            }else{
                Toast.makeText(this, "Dude, the camera is already activated..", Toast.LENGTH_SHORT).show()
            }
//            camView.setImageBitmap(mImgDepth.copy(Bitmap.Config.ARGB_8888, true))

//            Log.i(TAG, "Camera Thread starting")
//            mCamera.enable = true
//            mCamera.interval = 5
//            mVisionThread = Thread(mCamera, "CameraThread")
//            mVisionThread.start()
        } else {
            Toast.makeText(this, "Vision service not started yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera() {
        mVision.stopListenFrame(StreamType.DEPTH)
//        mCamera.enable = false
        cameraRunning = false
    }


}
