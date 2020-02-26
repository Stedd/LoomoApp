/**
 * A class that is meant to make the Intel RealSense available for the rest of the code
 */
package com.example.loomoapp.Loomo

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.calibration.Intrinsic
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException


class LoomoRealSense {
    companion object {
        const val TAG = "LoomoRealSense"

        const val COLOR_WIDTH = 640
        const val COLOR_HEIGHT = 480

        const val FISHEYE_WIDTH = 640
        const val FISHEYE_HEIGHT = 480

        const val DEPTH_WIDTH = 320
        const val DEPTH_HEIGHT = 240
    }

    var mVision = Vision.getInstance()
    private var waitingForServiceToBind = false

    private var mImgColor = Bitmap.createBitmap(COLOR_WIDTH, COLOR_HEIGHT, Bitmap.Config.ARGB_8888)
    private var mImgFishEye = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT, Bitmap.Config.ALPHA_8)
    private var mImgDepth = Bitmap.createBitmap(DEPTH_WIDTH, DEPTH_HEIGHT, Bitmap.Config.RGB_565)


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
                TAG,
                "Vision.isBind = ${mVision.isBind}${if (waitingForServiceToBind) ", but binding is in progress" else ""}"
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

    fun startColorCamera(threadHandler: Handler, imgBuffer: MutableLiveData<Bitmap>) {
        GlobalScope.launch {
            if (mVision.isBind) {
                try {
                    mVision.startListenFrame(
                        StreamType.COLOR
                    ) { streamType, frame ->
                        mImgColor.copyPixelsFromBuffer(frame.byteBuffer)
                        threadHandler.post {
                            imgBuffer.value = mImgColor
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    Log.d(
                        TAG,
                        "Exception in Vision.startListenFrame: Probably already listening to COLOR(1): $e"
                    )
                }
            } else if (!mVision.isBind and waitingForServiceToBind) {
                Log.d(TAG, "Waiting for service to bind before starting camera")
                while (!mVision.isBind) {
                }
                stopActiveCameras()
                startColorCamera(threadHandler, imgBuffer) // This recursion is safe.
            } else {
                Log.d(TAG, "Color camera not started. Bind Vision service first")
            }
        }
    }

    fun startFishEyeCamera(threadHandler: Handler, imgBuffer: MutableLiveData<Bitmap>) {
        GlobalScope.launch {
            if (mVision.isBind) {
                try {
                    mVision.startListenFrame(
                        StreamType.FISH_EYE
                    ) { streamType, frame ->
                        mImgFishEye.copyPixelsFromBuffer(frame.byteBuffer)
                        threadHandler.post {
                            imgBuffer.value = mImgFishEye
                        }
                    }
//                    val a = Intrinsic()
//                    Log.i(TAG, "Focal length: ${a.focalLength}, Distortion coef: ${a.distortion}, Principal: ${a.principal}")
                } catch (e: IllegalArgumentException) {
                    Log.d(
                        TAG,
                        "Exception in Vision.startListenFrame: Probably already listening to FISH_EYE(256): $e"
                    )
                }
            } else if (!mVision.isBind and waitingForServiceToBind) {
                Log.d(TAG, "Waiting for service to bind before starting camera")
                while (!mVision.isBind) {
                }
                stopActiveCameras()
                startFishEyeCamera(threadHandler, imgBuffer) // This recursion is safe.
            } else {
                Log.d(TAG, "FishEye cam not started. Bind Vision service first")
            }
        }
    }

    fun startDepthCamera(threadHandler: Handler, imgBuffer: MutableLiveData<Bitmap>) {
        GlobalScope.launch {
            if (mVision.isBind) {
                try {
                    mVision.startListenFrame(
                        StreamType.DEPTH
                    ) { streamType, frame ->
                        mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
                        threadHandler.post {
                            imgBuffer.value = mImgDepth
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    Log.d(
                        TAG,
                        "Exception in Vision.startListenFrame: Probably already listening to DEPTH(2): $e"
                    )
                }
            } else if (!mVision.isBind and waitingForServiceToBind) {
                Log.d(TAG, "Waiting for service to bind before starting camera")
                while (!mVision.isBind) {
                }
                stopActiveCameras()
                startDepthCamera(
                    threadHandler,
                    imgBuffer
                ) // This recursion is safe. The while loop on the previous line however...
            } else {
                Log.d(TAG, "Depth cam not started. Bind Vision service first")
            }
        }
    }
}
//
//package com.example.loomoapp.Loomo
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import android.widget.Toast
//import androidx.lifecycle.MutableLiveData
//import com.example.loomoapp.*
//import com.example.loomoapp.viewModel.MainActivityViewModel
//import com.segway.robot.sdk.base.bind.ServiceBinder
//import com.segway.robot.sdk.vision.Vision
//import com.segway.robot.sdk.vision.stream.StreamType
//
//class LoomoRealSense(viewModel: MainActivityViewModel) {
//    private val TAG = "LoomoRealsense"
//    val mVision :Vision = Vision.getInstance()
//    private val viewModel_ = viewModel
//
//    private var cameraRunning = false
//
//    private val imgWidthColor = 640
//    private val imgHeightColor = 480
//    private var mImgColor = Bitmap.createBitmap(
//        imgWidthColor,
//        imgHeightColor,
//        Bitmap.Config.ARGB_8888
//    )
//    private var mImgColorResult = Bitmap.createBitmap(
//        imgWidthColor,
//        imgHeightColor,
//        Bitmap.Config.ARGB_8888
//    )
//
//    private val imgWidthFishEye = 640
//    private val imgHeightFishEye = 480
//    private var mImgFishEye = Bitmap.createBitmap(
//        imgWidthFishEye,
//        imgHeightFishEye,
//        Bitmap.Config.ALPHA_8
//    )
//    private var mImgFishEyeResult = Bitmap.createBitmap(
//        imgWidthFishEye,
//        imgHeightFishEye,
//        Bitmap.Config.ARGB_8888
//    )
//
//    private val imgWidthDepth = 320
//    private val imgHeightDepth = 240
//    private var mImgDepth = Bitmap.createBitmap(
//        imgWidthDepth,
//        imgHeightDepth,
//        Bitmap.Config.RGB_565
//    )
//    private var mImgDepthResult = Bitmap.createBitmap(
//        imgWidthDepth,
//        imgHeightDepth,
//        Bitmap.Config.ARGB_8888
//    )
//
//    private var mImgDepthScaled =
//        Bitmap.createScaledBitmap(mImgDepth, imgWidthColor / 3, imgWidthColor / 3, false)
//
//
//    fun bind(context: Context){
//        // Bind Vision SDK service
//        mVision.bindService(context, object : ServiceBinder.BindStateListener {
//            override fun onBind() {
//                Log.d(TAG, "Vision onBind ${mVision.isBind}")
////                    mROSMain.startConsumers()
//            }
//
//            override fun onUnbind(reason: String?) {
//                Log.d(TAG, "Vision unBind. Reason: $reason")
//            }
//        })
//    }
//
//    fun startCamera(context: Context, msg: String) {
//        if (mVision.isBind) {
//            if (!cameraRunning) {
//                Log.i(TAG, msg)
//                // TODO: 25/02/2020 Separate individual cameras?
//                mVision.startListenFrame(StreamType.COLOR) { streamType, frame ->
//                    mImgColor.copyPixelsFromBuffer(frame.byteBuffer)
//                    viewModel_.realSenseColorImage = MutableLiveData(mImgColor)
//                }
//                mVision.startListenFrame(StreamType.FISH_EYE) { streamType, frame ->
//                    mImgFishEye.copyPixelsFromBuffer(frame.byteBuffer)
//                    viewModel_.realSenseFishEyeImage = MutableLiveData(mImgFishEye)
//                }
//                mVision.startListenFrame(StreamType.DEPTH) { streamType, frame ->
//                    mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
//                    viewModel_.realSenseDepthImage = MutableLiveData(mImgDepth)
//                }
//                cameraRunning = true
//            } else {
//                Toast.makeText(context, "Dude, the camera is already activated..", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        } else {
//            Toast.makeText(context, "Vision service not started yet", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    fun stopCamera(context: Context, msg: String) {
//        if (cameraRunning) {
//            Log.i(TAG, msg)
//            mVision.stopListenFrame(StreamType.COLOR)
//            cameraRunning = false
//        }
////        UIThreadHandler.post {
////            viewModel.image.setImageDrawable(getDrawable(context, R.drawable.ic_videocam))
////        }
//    }
//
//
//}