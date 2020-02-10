package com.example.loomoapp

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityViewModel

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

//    val mThread  = Thread("CalcThread")

//    val mRunnable2 = ExampleRunnable()

//    val mThread = ExampleThread()
    val mHandler = Handler()
    var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("asd", "Activity created")


//        val mThread2 = Thread(mRunnable2).start()
//        Log.i("asd", "Thread State Debug2: ${mThread2.}")

//        mThread.start()
//        mLooper.start()
//
//        if (mLooper.isAlive) {
//            Log.i("asd", "Thread started")
//        } else {
//            Log.i("asd", "Thread did not start")
//        }
//        Log.i("asd", "Thread State Debug2: ${mLooper.state}")

        btnStartService.setOnClickListener {
            startService()
        }
        btnStopService.setOnClickListener {
            stopService()
        }

        viewModel = ViewModelProvider(this)
            .get(MainActivityViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text = it
        })

        viewModel.text.value = "Service not started"

    }


    private fun startService() {
        Log.i("asd", "Service start command")
        index = 0
//        Thread(mRunnable).run()
        Thread(mRunnable).start()
//        newThread = Thread(mRunnable)
//        runOnUiThread(mRunnable)
//        mThread.run()

    }

    private fun stopService() {
        Log.i("asd", "Service stop command")
        mHandler.removeCallbacks(mRunnable)

//        mThread.runThreadMainLoop = false
//        Log.i("asd", "Thread State Debug3: ${mThread.state}")
        viewModel.text.value = "Service stopped"
    }


    private val mRunnable: Runnable = object : Runnable {
        override fun run() {
            index++
            Log.i("asd", "Index: $index. Looping on Thread: ${mHandler.looper.thread}")
//            viewModel.text.value = "Index: $index"
            mHandler.post(this)
        }
    }

//        private val mLooper: AsyncTask = object : Looper(){
//        override fun run() {
//            index++
//            Log.i("asd", "Index: $index. Looping on Thread: ${mHandler.looper.thread}")
////            viewModel.text.value = "Index: $index"
//            mHandler.post(this)
//        }
//    }



//    private val mThread = object : Thread("CalcThread"){
//
//        var runThreadMainLoop = false
//        override fun run() {
//            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
//            while (runThreadMainLoop){
////                            index++
//                Log.i("asd", "Index: $index. Looping on Thread: ${mHandler.looper.thread}")
////            viewModel.text.value = "Index: $index"
//                mHandler.postDelayed(this, 2000)
//            }
//            Log.i("asd", "Thread State Debug1: ${this.state}")
//        }
//    }


//    private val mThread = object : Thread(object : Runnable{
//        override fun run() {
////            index++
//            Log.i("asd", "Index: $index. Looping on Thread: ${mHandler.looper.thread}")
////            viewModel.text.value = "Index: $index"
//            mHandler.postDelayed(this,1000)
//        }
//    }){}

    class ExampleThread:Thread(){
        override fun run() {
            for (i in 0..10 ) {
                sleep(1000)
                Log.i("asd", "Index: $i. Looping on Thread: ${currentThread()}")
            }
            Log.i("asd", "Thread state: ${this.state}")
        }
    }

    class ExampleRunnable:Runnable{
        override fun run() {
            for (i in 0..10 ) {
                Thread.sleep(1000)
                Log.i("asd", "Index: $i. Looping on Thread: ${Thread.currentThread()}")
            }
        }
    }

}
