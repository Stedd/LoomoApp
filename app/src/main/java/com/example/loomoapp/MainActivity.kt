package com.example.loomoapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.perception.sensor.Sensor
import com.segway.robot.sdk.vision.Vision
import kotlinx.android.synthetic.main.activity_main.*

//Variables
private lateinit var viewModel: MainActivityViewModel
private val TAG = "asd"
private lateinit var mBase: Base
private lateinit var mVision: Vision
private lateinit var mLoomoSensor: LoomoSensor

class MainActivity : AppCompatActivity() {

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    private val mRunnable = ExampleRunnable()

    private var mThread = Thread(mRunnable, "CalcThread")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("asd", "Activity created")

        mBase = Base.getInstance()
        mVision = Vision.getInstance()
        mLoomoSensor = LoomoSensor(this)

        //Start thread when starting up
        mThread.start()

        viewModel = ViewModelProvider(this)
            .get(MainActivityViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

        viewModel.text.value = "Service not started"

        if (mThread.isAlive) {
            Log.i("asd", "Thread started")
            viewModel.text.value = "Thread started"
        } else {
            Log.i("asd", "Thread not started")
            viewModel.text.value = "Thread not started"
        }

        btnStartService.setOnClickListener {
            startService()
        }
        btnStopService.setOnClickListener {
            stopService()
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
        if (mThread.isAlive) {
            Log.i("asd", "App closed, Thread stopping")
            mRunnable.runThread = false
        }
        super.onDestroy()
    }

    private fun startService() {
        if (mThread.isAlive) {
//            Log.i("asd", "Thread start command")
////            viewModel.text.value = "Thread started"
//            mRunnable.runThread = true
//            mThread.run()
        } else {
            Log.i("asd", "Thread is not running, starting")
            mRunnable.runThread = true
            mThread = Thread(mRunnable, "ControllerThread")
            mThread.start()
        }
    }

    private fun stopService() {
        Log.i("asd", "Service stop command")
        mRunnable.runThread = false
        viewModel.text.value = "Thread stopped"
    }

    class ExampleRunnable : Runnable {

        var runThread = false
        var error: Float = 0.0F
        var dist: Float = 0.0F
        val gain: Float = 0.001F
        val setpoint: Float = 500.0F
        private val threadHandler =
            Handler(Looper.getMainLooper()) //Used to post messages to UI Thread

        override fun run() {
            while (runThread) {
                //Logic
                dist = mLoomoSensor.getSurroundings().UltraSonic.toFloat()
//                Log.i("asd", "Ultrasonic: $dist. ${Thread.currentThread()}")
                error = (setpoint - dist) * gain * -1.0f
                mBase.setLinearVelocity(error)

                //Check for stop signal
                if (!runThread) {
                    mBase.setLinearVelocity(0.0F)
                    break
                }

                //Post variables to UI
                threadHandler.post {
                    viewModel.text.value = "Lin_Vel: $error"
                }

                //Thread interval
                Thread.sleep(10)
            }
            Log.i(
                "asd",
                "${Thread.currentThread()} terminated."
            )
        }
    }
}
