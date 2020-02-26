package com.example.loomoapp

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.example.loomoapp.Loomo.LoomoBase
import com.example.loomoapp.Loomo.LoomoControl
import com.example.loomoapp.Loomo.LoomoRealsense
import com.example.loomoapp.Loomo.LoomoSensor
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.ROSService
import com.example.loomoapp.viewModel.MainActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.ros.android.AppCompatRosActivity
import org.ros.android.RosActivity
import org.ros.node.NodeMainExecutor
import java.net.URI


public class MainActivity : AppCompatRosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.31:11311/")) {
//public class MainActivity : AppCompatActivity() {

    private val UIThreadHandler = Handler() //Used to post messages to UI Thread
//    private lateinit var viewModelStoreOwner: ViewModelStoreOwner
//    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModel: MainActivityViewModel


    //Variables
    private val TAG = "MainActivity"

    //Initialize loomo classes
    private lateinit var mLoomoBase: LoomoBase
    private lateinit var mLoomoRealsense: LoomoRealsense
    private lateinit var mLoomoSensor: LoomoSensor
    private lateinit var mLoomoControl: LoomoControl

    //ROS classes
    private lateinit var mROSMain: ROSService
    private lateinit var intentROS: Intent

    //OpenCV Variables
    private lateinit var mOpenCVMain: OpenCVMain
    private lateinit var intentOpenCV: Intent

    //Import native functions
    private external fun stringFromJNI(): String

    private val textView by lazy {
        findViewById<TextView>(R.id.textView)
    }

    init {
        Log.d(TAG, "Init Main activity")

        //Load native
        System.loadLibrary("native-lib")

    }

    private fun getLifecycleOwner(): LifecycleOwner {
        var context: Context = this
        while (context !is LifecycleOwner) {
            context = (context as ContextWrapper).baseContext
        }
        return context
    }

    override fun init(p0: NodeMainExecutor?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "Activity created")
//        viewModelStore = ViewModelStore()
//        viewModelStoreOwner = ViewModelStoreOwner { this.viewModelStore }
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        //Initialize classes
        mLoomoBase      = LoomoBase     (viewModel)
        mLoomoRealsense = LoomoRealsense(viewModel)
        mLoomoSensor    = LoomoSensor   (viewModel)
        mLoomoControl   = LoomoControl  (viewModel, mLoomoBase, mLoomoSensor)

//        mROSMain        = ROSService(UIThreadHandler, mLoomoBase, mLoomoSensor, mLoomoRealsense)
        mROSMain        = ROSService()
        intentROS       = Intent(this, mROSMain::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)


        mOpenCVMain     = OpenCVMain()
        intentOpenCV    = Intent(this, mOpenCVMain::class.java)

        //Start OpenCV Service
        startService(intentOpenCV)

        //Start Ros Service
//        startService(intentROS)
//        mROSMain.startROS(UIThreadHandler, mLoomoBase, mLoomoSensor, mLoomoRealsense)

        mLoomoControl.mControllerThread.start()

        mOpenCVMain.onCreate(this, findViewById(R.id.javaCam))

        viewModel.text.observe(getLifecycleOwner(), Observer {
            textView.text = it
        })

        viewModel.realSenseColorImage.observe(getLifecycleOwner(), Observer {
            camView.setImageBitmap(it)
        })


        viewModel.text.value = "Service not started"

        // Onclicklisteners
        btnStartService.setOnClickListener {
            mLoomoControl.startController(this,"ControllerThread start command")
        }
        btnStopService.setOnClickListener {
            mLoomoControl.stopController(this,"ControllerThread stop command")
        }
        btnStartCamera.setOnClickListener {
            mLoomoRealsense.startCamera(this, "Camera start command")
        }
        btnStopCamera.setOnClickListener {
            mLoomoRealsense.stopCamera(this,"Camera stop command")
        }

        //Helloworld from c++
        sample_text.text = stringFromJNI()
    }

    override fun onResume() {

        mOpenCVMain.resume()

//        UIThreadHandler.postDelayed({ mROSMain.startConsumers()}, 5000)

        //Bind Sensor SDK service
        mLoomoSensor.bind(this)

        mLoomoRealsense.bind(this)

        mLoomoBase.bind(this)

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
        mLoomoControl.stopController(this,"App paused, Controller thread stopping")

        mLoomoRealsense.stopCamera(this, "App paused, Camera thread stopping")

    }

}


