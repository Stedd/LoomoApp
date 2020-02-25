/**
 * A class that is meant to make the Intel RealSense available for the rest of the code
 */
package com.example.loomoapp.Loomo

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LoomoRealSense(context: Context, receivingThreadHandler: Handler) {
    companion object {
        const val TAG = "LoomoRealSense"

        const val COLOR_WIDTH = 640
        const val COLOR_HEIGHT = 480

        const val FISHEYE_WIDTH = 640
        const val FISHEYE_HEIGHT = 480

        const val DEPTH_WIDTH = 320
        const val DEPTH_HEIGHT = 240
    }

    private val handler_ = receivingThreadHandler

    var mVision = Vision.getInstance()

    private var colorIsActive = false
    private var fishEyeIsActive = false
    private var depthIsActive = false

    var mImgColor = Bitmap.createBitmap(COLOR_WIDTH, COLOR_HEIGHT, Bitmap.Config.ARGB_8888)
    var mImgFishEye = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT, Bitmap.Config.ALPHA_8)
    var mImgDepth = Bitmap.createBitmap(DEPTH_WIDTH, DEPTH_HEIGHT, Bitmap.Config.RGB_565)

    init {
        mVision.bindService(context.applicationContext, object : ServiceBinder.BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Vision onBind")
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Vision onUnbind")
            }
        })
    }

    fun setActiveStateOfCameras(color: Boolean, fishEye: Boolean, depth: Boolean) {
        colorIsActive = color
        fishEyeIsActive = fishEye
        depthIsActive = depth
        stopActiveCameras()
        startActiveCameras()
    }

    private fun startActiveCameras() {
        if (colorIsActive || fishEyeIsActive || depthIsActive) {
            GlobalScope.launch {
                if (colorIsActive) startColorCamera()
                if (fishEyeIsActive) startFishEyeCamera()
                if (depthIsActive) startDepthCamera()
            }
        }
    }

    private fun stopActiveCameras() {
        if (mVision.isBind) {
            mVision.stopListenFrame(StreamType.COLOR)
            mVision.stopListenFrame(StreamType.FISH_EYE)
            mVision.stopListenFrame(StreamType.DEPTH)
        }
    }

    private suspend fun startColorCamera() {
        while (!mVision.isBind) {
            delay(10L)
        }
        mVision.startListenFrame(
            StreamType.COLOR
        ) { streamType, frame -> mImgColor.copyPixelsFromBuffer(frame.byteBuffer) }
    }

    private suspend fun startFishEyeCamera() {
        while (!mVision.isBind) {
            delay(10L)
        }
        mVision.startListenFrame(
            StreamType.FISH_EYE
        ) { streamType, frame -> mImgFishEye.copyPixelsFromBuffer(frame.byteBuffer) }
    }

    private suspend fun startDepthCamera() {
        while (!mVision.isBind) {
            delay(10L)
        }
//        mVision.startListenFrame(
//            StreamType.DEPTH
//        ) { streamType, frame -> mImgDepth.copyPixelsFromBuffer(frame.byteBuffer) }
        mVision.startListenFrame(StreamType.DEPTH, object : Vision.FrameListener {
            override fun onNewFrame(streamType: Int, frame: Frame) {
                mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
                val msg = Message.obtain(handler_)
                handler_.sendMessage(msg)
            }
        })
    }
}