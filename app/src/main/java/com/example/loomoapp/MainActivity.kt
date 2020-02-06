package com.example.loomoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityViewModel
    private var incrementRunnable = IncrementValueRunnable()

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

//    var myService : ValueReader? = null
//    var isBound = false

//    var myConnection = object : ServiceConnection {
//        override fun onServiceConnected(className: ComponentName,
//                                                    service: IBinder) {
//            val binder = service as ValueReader.LocalBinder
//            myService = binder.getService()
//            isBound = true
//        }
//
//        override fun onServiceDisconnected(className: ComponentName) {
//            isBound = false
//        }
//    }

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
//        val intent = Intent(this, ValueReader::class.java)
//        startService(intent)
//        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        startThread(true)
//        updateDisplay()
    }

    private fun stopService (){
//        if (myService?.isBound == true){
//            Log.i("asd", "Service stop command")
//            unbindService(myConnection)
//            updateDisplay()
//        }
        startThread(false)
//        textView.text= "service stopped"
    }

    fun startThread(bool:Boolean){
        if (bool){
            incrementRunnable.run()
        }
    }


//    fun updateDisplay(){
//        viewModel.text.value = myService?.message
//    }



}




