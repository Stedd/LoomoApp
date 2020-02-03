package com.example.loomoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.ViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }


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
            .get(ViewModel::class.java)

        viewModel.text.observe(this, Observer {
            textView.text= it
        })


    }

    private fun startService(){
        Log.i("asd", "Service start command")
        val intent = Intent(this, ValueReader::class.java)
        startService(intent)
        //bindService(intent, )
//        textView.text= "service started"
    }

    private fun stopService (){
        Log.i("asd", "Service stop command")
        val intent = Intent(this, ValueReader::class.java)
        stopService(intent)
//        textView.text= "service stopped"
    }
}




