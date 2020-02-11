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

    private val mThread = Thread(mRunnable, "CalcThread")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("asd", "Activity created")

        mBase = Base.getInstance()
        mVision = Vision.getInstance()
        mLoomoSensor = LoomoSensor(this)

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

//        mSensor.bindService(this.applicationContext, object : ServiceBinder.BindStateListener {
//            override fun onBind() {
//                Log.d(TAG, "sensor onBind")
//            }
//
//            override fun onUnbind(reason: String) {
//            }
//        })

        super.onResume()
    }

    private fun startService() {
        if (mThread.isAlive) {
            Log.i("asd", "Thread start command")
//            viewModel.text.value = "Thread started"
            mRunnable.runThread = true
        } else {
            Log.i("asd", "Thread is dead, starting")
            mThread.start()
            startService()
        }
//        viewModel.text.value = "Thread started: ${mLoomoSensor.getSensBaseImu().pitch}"
    }

    private fun stopService() {
        Log.i("asd", "Service stop command")
        mRunnable.runThread = false
        viewModel.text.value = "Thread stopped"
    }

    class ExampleRunnable : Runnable {

        var runThread = false
        var error:Float = 0.0F
        var dist:Float = 0.0F
        val gain : Float = 0.001F
        val setpoint: Float = 500.0F
        private val threadHandler = Handler(Looper.getMainLooper()) //Used to post messages to UI Thread

        override fun run() {
            if (runThread) {
                dist = mLoomoSensor.getSurroundings().UltraSonic.toFloat()
                Log.i("asd", "Ultrasonic: $dist")
                error = (setpoint-dist)*gain*-1.0f
                mBase.setLinearVelocity(error)
                threadHandler.post {
                    viewModel.text.value = "Lin_Vel: $error"
                }
                Thread.sleep(5)
                run()
            } else {
                //Sleep thread until it is given a task
                Thread.sleep(250)
                mBase.setLinearVelocity(0.0F)
                Log.i("asd", "Keeping ${Thread.currentThread()} alive")
                run()
            }
        }
    }
}
