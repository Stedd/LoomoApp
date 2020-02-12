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
import java.lang.System.currentTimeMillis
import kotlin.math.sin

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
            Log.i("asd", "App destroyed, Thread stopping")
            mRunnable.runThread = false
        }
        super.onDestroy()
    }


    override fun onPause() {
        if (mThread.isAlive) {
            Log.i("asd", "App paused, Thread stopping")
            mRunnable.runThread = false
        }
        super.onPause()
    }

    private fun startService() {
        if (!mThread.isAlive) {
            Log.i("asd", "ControllerThread starting")
            mRunnable.runThread = true
            mThread = Thread(mRunnable, "ControllerThread")
            mThread.start()
        }
    }

    private fun stopService() {
        Log.i("asd", "Service stop command")
        mRunnable.runThread = false

    }

    class ExampleRunnable : Runnable {

        var runThread = false
        var error: Float = 0.0F
        var turnError = 0.0F
        var startTime = currentTimeMillis()
        var dist: Float = 0.0F
        val gain: Float = 0.002F
        var setpoint: Float = 500.0F
        private val threadHandler =
            Handler(Looper.getMainLooper()) //Used to post messages to UI Thread

        override fun run() {
            while (runThread) {
                //Logic

                setpoint = 500.0F + (150.0F* sin((startTime-currentTimeMillis()).toFloat()*2E-4F))
//                Log.i("asd", "setp: ${sin((startTime-currentTimeMillis()).toFloat()*2E-4)}. ${Thread.currentThread()}")

                dist = mLoomoSensor.getSurroundings().UltraSonic.toFloat()
//                Log.i("asd", "Ultrasonic: $dist. ${Thread.currentThread()}")
                error = (setpoint - dist) * gain * -1.0f
//                turnError = (mLoomoSensor.getSurroundings().IR_Left.toFloat()-
//                            mLoomoSensor.getSurroundings().IR_Right.toFloat())*
//                            -0.01F
                //Set velocity
                mBase.setLinearVelocity(error)
                mBase.setAngularVelocity(turnError)

                //Post variables to UI
                threadHandler.post {
                    viewModel.text.value =
                            "Distance controller\n" +
                            "setp:$setpoint\n" +
                            "dist:$dist\n" +
                            "Lin_Vel: $error\n" +
                            "Ang_Vel:$turnError"
                }

                //Thread interval
                Thread.sleep(10)

                //Check for stop signal
                if (!runThread) {
                    mBase.setLinearVelocity(0.0F)
                    threadHandler.post {
                        viewModel.text.value = "Thread stopped"
                    }
                    break
                }
            }
            Log.i(
                "asd",
                "${Thread.currentThread()} stopped."
            )
        }
    }
}
