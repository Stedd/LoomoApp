package com.example.loomoapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityViewModel

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

//    val mRun



    val mRunnable = ExampleRunnable()

    private val mThread  = Thread(mRunnable,"CalcThread")

//    val mThread = ExampleThread()
//    val mHandler = Handler(mThread)
    var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("asd", "Activity created")

        viewModel = ViewModelProvider(this)
            .get(MainActivityViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

        viewModel.text.value = "Service not started"

//        val mThread2 = Thread(mRunnable2).start()
//        Log.i("asd", "Thread State Debug2: ${mThread2.}")

//        mThread.start()
//        mLooper.start()
//
        if (mThread.isAlive) {
            Log.i("asd", "Thread started")
            viewModel.text.value = "Thread started"
        } else {
            Log.i("asd", "Thread did not start")
            viewModel.text.value = "Thread did not start"
        }
//        Log.i("asd", "Thread State Debug2: ${mLooper.state}")

        btnStartService.setOnClickListener {
            startService()
        }
        btnStopService.setOnClickListener {
            stopService()
        }

    }


    private fun startService() {
        if (mThread.isAlive){
            Log.i("asd", "Thread start command")
            viewModel.text.value = "Thread started"
            mRunnable.runThread = true
        }else{
            Log.i("asd", "Thread is dead, starting")
            mThread.start()
            startService()
        }


        viewModel.text.value = "Thread started: ${mRunnable.value}"

    }

    private fun stopService() {
        Log.i("asd", "Service stop command")
        mRunnable.runThread = false
        viewModel.text.value = "Thread stopped"
    }

    class ExampleRunnable:Runnable{


        var runThread = false
        var value = 0

        override fun run() {
            if (runThread) {
                for (i in 0..3000) {
                    Thread.sleep(5)
                    value ++
                    Log.i("asd", "Index: $value. Looping on ${Thread.currentThread()}")
//                    viewModel.text.value = "Thread started: ${mRunnable.value}"
                    if (!runThread){
                        break
                    }
                }
                runThread = false
                value = 0
                run()
            }
            else{
                Thread.sleep(250)
                Log.i("asd", "Keeping ${Thread.currentThread()} alive")
                run()
            }
        }
    }

//    internal class LooperThread : Thread() {
//        var mHandler: Handler? = null
//        override fun run() {
//            Looper.prepare()
//            mHandler = @SuppressLint("HandlerLeak")
//            object : Handler() {
//                override fun handleMessage(msg: Message?) { // process incoming messages here
//                }
//            }
//            Looper.loop()
//        }
//    }

}
