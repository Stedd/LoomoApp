/**
 * A class that makes the Intel RealSense available for the rest of the code
 */
package com.example.loomoapp.Loomo

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.OpenCV.OpenCVMain
import com.example.loomoapp.ROS.RealsensePublisher
import com.example.loomoapp.utils.RingBuffer
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


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
                        stopActiveCameras()
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


    fun stopActiveCameras() {
        if (mVision.isBind) {
            mVision.stopListenFrame(StreamType.COLOR)
            mVision.stopListenFrame(StreamType.FISH_EYE)
            mVision.stopListenFrame(StreamType.DEPTH)
        }
    }


    fun startColorCamera(
        threadHandler: Handler,
        receiverBuffer: MutableLiveData<ByteBuffer>,
        receiverInfo: MutableLiveData<FrameInfo>
    ) {
        startCamera(StreamType.COLOR, threadHandler, receiverBuffer, receiverInfo)
    }

    fun startFishEyeCamera(
        threadHandler: Handler,
        receiverBuffer: MutableLiveData<ByteBuffer>,
        receiverInfo: MutableLiveData<FrameInfo>
    ) {
        startCamera(StreamType.FISH_EYE, threadHandler, receiverBuffer, receiverInfo)
    }

    fun startDepthCamera(
        threadHandler: Handler,
        receiverBuffer: MutableLiveData<ByteBuffer>,
        receiverInfo: MutableLiveData<FrameInfo>
    ) {
        startCamera(StreamType.DEPTH, threadHandler, receiverBuffer, receiverInfo)
    }

    //TODO: check that 'receiver' is unique (two camera streams writing to the same var has caused crashes

    @Suppress("ControlFlowWithEmptyBody")
    private fun startCamera(
        streamType: Int,
        threadHandler: Handler,
        receiverBuffer: MutableLiveData<ByteBuffer>,
        receiverInfo: MutableLiveData<FrameInfo>

    ) {
        GlobalScope.launch {
            when {
                mVision.isBind -> {
                    try {
                        mVision.startListenFrame(streamType)
                        { streamType, frame ->
                            threadHandler.post {
                                receiverBuffer.value = frame.byteBuffer
                                receiverInfo.value = frame.info
                            }
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
                    startCamera(
                        streamType,
                        threadHandler,
                        receiverBuffer,
                        receiverInfo
                    ) // This recursion is safe.
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
