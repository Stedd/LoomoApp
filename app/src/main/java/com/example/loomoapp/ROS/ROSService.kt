package com.example.loomoapp.ROS

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import com.example.loomoapp.Loomo.LoomoBase
import com.example.loomoapp.Loomo.LoomoRealsense
import com.example.loomoapp.Loomo.LoomoSensor

class ROSService() : Service() {

//    private val base_ = base
//    private val sensor_ = sensor
//    private val vision_ = realsense
//    private val handler_ = handler



    //ROS classes
    private lateinit var mROSMain: ROSMain
//    private lateinit var intentROS: Intent

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    fun startROS(
        handler: Handler,
        base: LoomoBase,
        sensor: LoomoSensor,
        realsense: LoomoRealsense
    ) {
        mROSMain = ROSMain(handler, base, sensor, realsense)
        mROSMain.initMain()
    }
}