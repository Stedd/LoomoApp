package com.example.loomoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityViewModel

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    val mHandler = Handler()

    var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("asd", "Activity created")

        btnStartService.setOnClickListener {
            startService()
        }
        btnStopService.setOnClickListener {
            stopService()
        }

        viewModel = ViewModelProvider(this)
            .get(MainActivityViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text= it
        })

        viewModel.text.value = "Service not started"

    }




    private fun startService(){
        Log.i("asd", "Service start command")
        index = 0
        mToastRunnable.run()
    }

    private fun stopService (){
        Log.i("asd", "Service stop command")
        mHandler.removeCallbacks(mToastRunnable)
        viewModel.text.value = "Service stopped"
    }


    val mToastRunnable: Runnable = object : Runnable {
        override fun run() {
            index++
            Log.i("asd", "Index: $index. Looping on Thread: ${mHandler.looper.thread}")
            viewModel.text.value = "Index: $index"
            mHandler.post(this)
        }
    }
}




