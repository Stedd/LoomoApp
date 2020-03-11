/**
 * A class that makes the Intel RealSense available for the rest of the code
 */
package com.example.loomoapp.Loomo

import android.content.Context
import android.util.Log
import com.example.loomoapp.ROS.RealsensePublisher
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LoomoRealSense(private val publisher_: RealsensePublisher) {

    companion object {
        const val TAG = "LoomoRealSense"

        const val COLOR_WIDTH = 640
        const val COLOR_HEIGHT = 480

        const val FISHEYE_WIDTH = 640
        const val FISHEYE_HEIGHT = 480

        const val DEPTH_WIDTH = 320
        const val DEPTH_HEIGHT = 240

        val streamTypeMap = mapOf(
            1 to "Color",
            2 to "Depth",
            4 to "IR",
            8 to "Left",
            16 to "Right",
            32 to "Ext Depth L",
            64 to "Ext Depth R",
            256 to "Fish Eye"
        )
    }

    var mVision: Vision = Vision.getInstance()
    private var waitingForServiceToBind = false

    fun bind(context: Context) {
        if (!mVision.isBind and !waitingForServiceToBind) {
            Log.d(TAG, "Started Vision.bindService")
            waitingForServiceToBind = true
            mVision.bindService(
                context,
                object : ServiceBinder.BindStateListener {
                    override fun onBind() {
                        Log.d(TAG, "Vision onBind")
                        waitingForServiceToBind = false
                    }

                    override fun onUnbind(reason: String?) {
                        Log.d(TAG, "Vision onUnbind")
                        stopCameras()
                    }
                })
        } else {
            Log.d(
                TAG, "Vision.isBind = ${mVision.isBind}" +
                        if (waitingForServiceToBind) ", but binding is in progress" else ""
            )
        }
        publisher_.setVision(mVision)
    }


    fun stopCameras() {
        if (mVision.isBind) {
            mVision.stopListenFrame(StreamType.COLOR)
            mVision.stopListenFrame(StreamType.FISH_EYE)
            mVision.stopListenFrame(StreamType.DEPTH)
        }
    }

    fun startCameras(callback: (streamType: Int, frame: Frame) -> Unit) {
        startCamera(StreamType.FISH_EYE, callback)
        startCamera(StreamType.COLOR, callback)
        startCamera(StreamType.DEPTH, callback)
    }

    @Suppress("ControlFlowWithEmptyBody")
    private fun startCamera(streamType: Int, callback: (streamType: Int, frame: Frame) -> Unit) {
        GlobalScope.launch {
            when {
                mVision.isBind -> {
                    try {
                        mVision.startListenFrame(streamType)
                        { streamType, frame ->
                            callback(streamType, frame)
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.d(
                            TAG,
                            "Exception in Vision.startListenFrame ignored because probably already listening ($streamType = ${streamTypeMap[streamType]}): $e"
                        )
                    }
                }
                !mVision.isBind and waitingForServiceToBind -> {
                    Log.d(
                        TAG,
                        "Waiting for service to bind before starting ${streamTypeMap[streamType]} camera"
                    )
                    while (!mVision.isBind) {
                    }
                    mVision.stopListenFrame(streamType)
                    startCamera(streamType, callback) // This recursion is safe.
                }
                else -> {
                    Log.d(
                        TAG,
                        "${streamTypeMap[streamType]} not started. Bind Vision service first"
                    )
                }
            }
        }
    }
}
