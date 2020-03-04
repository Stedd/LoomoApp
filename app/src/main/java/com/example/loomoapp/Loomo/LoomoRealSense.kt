/**
 * A class that makes the Intel RealSense available for the rest of the code
 */
package com.example.loomoapp.Loomo

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.calibration.ColorDepthCalibration
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


class LoomoRealSense {
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
            4 to "Left",
            16 to "Right",
            32 to "Ext Depth L",
            64 to "Ext Depth R",
            256 to "Fish Eye"
        )
    }

    var mVision = Vision.getInstance()
    private var waitingForServiceToBind = false

    fun bind(context: Context) {
        if (!mVision.isBind and !waitingForServiceToBind) {
            Log.d(TAG, "Started Vision.bindService")
            waitingForServiceToBind = true
            mVision.bindService(
                context.applicationContext,
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
    }


    fun stopActiveCameras() {
        if (mVision.isBind) {
            mVision.stopListenFrame(StreamType.COLOR)
            mVision.stopListenFrame(StreamType.FISH_EYE)
            mVision.stopListenFrame(StreamType.DEPTH)
        }
    }

    fun startColorCamera(threadHandler: Handler, receiver: MutableLiveData<ByteBuffer>) {
        startCamera(StreamType.COLOR, threadHandler, receiver)
    }

    fun startFishEyeCamera(threadHandler: Handler, receiver: MutableLiveData<ByteBuffer>) {
        startCamera(StreamType.FISH_EYE, threadHandler, receiver)
    }

    fun startDepthCamera(threadHandler: Handler, receiver: MutableLiveData<ByteBuffer>) {
        startCamera(StreamType.DEPTH, threadHandler, receiver)
    }



    @Suppress("ControlFlowWithEmptyBody")
    private fun startCamera(
        streamType: Int,
        threadHandler: Handler,
        receiver: MutableLiveData<ByteBuffer>
    ) {
        GlobalScope.launch {
            when {
                mVision.isBind -> {
                    try {
                        mVision.startListenFrame(streamType)
                        { streamType, frame ->
                            threadHandler.post {
                                receiver.value = copyBuffer(frame.byteBuffer)
//                                receiver.value.position(frame.byteBuffer.position())
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
                    Log.d(TAG, "Waiting for service to bind before starting ${streamTypeMap[streamType]} camera")
                    while (!mVision.isBind) {
                    }
                    mVision.stopListenFrame(streamType)
                    startCamera(streamType, threadHandler, receiver) // This recursion is safe.
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

    fun getByteBufferAsByteArray(src: ByteBuffer): ByteArray {
        val bytesInBuffer = src.remaining()
        val tmpArr = ByteArray(bytesInBuffer) { src.get() }
        src.rewind()
        return tmpArr
    }

    private fun copyBuffer(src: ByteBuffer): ByteBuffer {
        val copy = ByteBuffer.allocate(src.capacity())
        src.rewind()
        copy.put(src)
        src.rewind()
        copy.flip()
        return copy
    }
}
