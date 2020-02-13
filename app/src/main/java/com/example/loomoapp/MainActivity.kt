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
const val TAG = "debugMSG"

lateinit var viewModel: MainActivityViewModel
lateinit var mBase: Base
lateinit var mVision: Vision
lateinit var mLoomoSensor: LoomoSensor
val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread
var cameraRunning: Boolean = false

class MainActivity : AppCompatActivity() {

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    private val imgWidth = 320
    private val imgHeight = 240
    private var mImgDepth = Bitmap.createBitmap(
        imgWidth,
        imgHeight,
        Bitmap.Config.RGB_565
    ) // Depth info is in Z16 format. RGB_565 is also a 16 bit format and is compatible for storing the pixels

    private val mDistanceController = DistanceController()
    private var mControllerThread = Thread()

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

        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))

        viewModel.text.value = "Service not started"

        btnStartService.setOnClickListener {
            startController("ControllerThread start command")
            Log.d(TAG, "${HelloWorld()}")
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
        }else {
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

    private fun startCamera(msg: String) {
        if (mVision.isBind) {
            if (!cameraRunning) {
                Log.i(TAG, msg)
                mVision.startListenFrame(StreamType.DEPTH) { streamType, frame ->
                    mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
                    threadHandler.post {
                        camView.setImageBitmap(mImgDepth.copy(Bitmap.Config.ARGB_8888, true))
                    }
                }
                cameraRunning = true
            } else {
                Toast.makeText(this, "Dude, the camera is already activated..", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "Vision service not started yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera(msg: String) {
        if (cameraRunning) {
            Log.i(TAG, msg)
            mVision.stopListenFrame(StreamType.DEPTH)
            cameraRunning = false
            camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))
        }
    }

}
