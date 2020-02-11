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
import kotlinx.android.synthetic.main.activity_main.*

//Variables
private lateinit var viewModel: MainActivityViewModel

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
        viewModel.text.value = "Thread started: ${mRunnable.value}"
    }

    private fun stopService() {
        Log.i("asd", "Service stop command")
        mRunnable.runThread = false
        viewModel.text.value = "Thread stopped"
    }

    class ExampleRunnable : Runnable {

        var runThread = false
        var value = 0
        private val threadHandler = Handler(Looper.getMainLooper())

        override fun run() {
            if (runThread) {
                threadHandler.post {
                    Log.i("asd", "Thread Running")
                    viewModel.text.value = "Thread Running"
                }

                for (i in 0..10) {
                    Thread.sleep(250)
                    value++

                    threadHandler.post {
                            Log.i("asd", "Index: $value. Looping on ${Thread.currentThread()}")
                            viewModel.text.value = "Index: $value"
                    }

                    if (!runThread) {
                        break
                    }
                }
                runThread = false
                value = 0
                threadHandler.post {
                    Log.i("asd", "Thread Finished")
                    viewModel.text.value = "Thread finished"
//                            textView.text = "Thread started: $value"
                }
                run()
            } else {
                Thread.sleep(250)
                Log.i("asd", "Keeping ${Thread.currentThread()} alive")
                run()
            }
        }
    }
}
