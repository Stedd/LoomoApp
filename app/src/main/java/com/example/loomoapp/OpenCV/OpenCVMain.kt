package com.example.loomoapp.OpenCV

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.LoopedThread
import com.example.loomoapp.utils.RingBuffer
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.Scalar
import org.opencv.features2d.Features2d

class OpenCVMain : Service() {
    private val TAG = "OpenCVMain"

    init {
        //Load native
//        System.loadLibrary("native-opencv")
//        System.loadLibrary("native-lib")
        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("$TAG init", "OpenCV not loaded")
        } else {
            Log.d("$TAG init", "OpenCV loaded")
        }
    }

    private lateinit var mLoaderCallback: BaseLoaderCallback

    private var fishEyeFrameBuffer = RingBuffer<Pair<Mat, FrameInfo>>(30, true)
    private var colorFrameBuffer = RingBuffer<Pair<Mat, FrameInfo>>(30, true)
    private var depthFrameBuffer = RingBuffer<Pair<Mat, FrameInfo>>(30, true)


    private val fishEyeTracker = ORBTracker()
    var captureNewFrame = true

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    fun onCreate(context: Context) {
        mLoaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> Log.d(TAG, "OpenCV loaded successfully")
                    else -> super.onManagerConnected(status)
                }
            }
        }
    }

    fun resume() {
        //Start OpenCV
        Log.d(TAG, "Activity resumed")
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private val imgProcFishEye = LoopedThread("imgProc FishEye", Process.THREAD_PRIORITY_DEFAULT)
    private var keyPoints = MatOfKeyPoint()
//    private external fun nativeOrb(matAddr: Long, dstAddr: Long)


    fun onNewFrame(streamType: Int, frame: Frame) {
//        val tic = System.currentTimeMillis()
        when (streamType) {
            StreamType.FISH_EYE -> {
                fishEyeFrameBuffer.enqueue(
                    Pair(
                        frame.byteBuffer.toMat(
                            FISHEYE_WIDTH, FISHEYE_HEIGHT,
                            CV_8UC1
                        ), frame.info
                    )
                )
                if (captureNewFrame) {
                    captureNewFrame = false
                    imgProcFishEye.handler.post {
                        keyPoints = fishEyeTracker.onNewFrame(fishEyeFrameBuffer.peekTail()!!.first)
//                        nativeOrb(fishEyeFrameBuffer.peekTail()!!.first.nativeObjAddr, keyPoints.nativeObjAddr)
                    }
                }
                Features2d.drawKeypoints(
                    fishEyeFrameBuffer[fishEyeFrameBuffer.tail]!!.first,
                    keyPoints,
                    fishEyeFrameBuffer[fishEyeFrameBuffer.tail]!!.first,
                    Scalar(0.0, 255.0, 0.0)
                )
            }
            StreamType.COLOR -> {
                colorFrameBuffer.enqueue(
                    Pair(
                        frame.byteBuffer.toMat(
                            COLOR_WIDTH, COLOR_HEIGHT,
                            CV_8UC4
                        ), frame.info
                    )
                )
            }
            StreamType.DEPTH -> {
                depthFrameBuffer.enqueue(
                    Pair(
                        frame.byteBuffer.toMat(
                            DEPTH_WIDTH, DEPTH_HEIGHT,
//                            CV_16UC1
                            CV_8UC2
                        ), frame.info
                    )
                )
            }
            else -> {
                throw IllegalStreamTypeException("Stream type not recognized in onNewFrame")
            }
        }
//        val toc = System.currentTimeMillis()
//        Log.d(TAG, "${streamTypeMap[streamType]} frame receive time: ${toc - tic}ms")
    }


    fun getNewestFrame(streamType: Int, callback: (Bitmap) -> Unit) {
        val frame: Mat? = when (streamType) {
            StreamType.FISH_EYE -> fishEyeFrameBuffer.peek(1)?.first
            StreamType.COLOR -> colorFrameBuffer.peek(1)?.first
            StreamType.DEPTH -> depthFrameBuffer.peek(1)?.first
            else -> throw IllegalStreamTypeException("Non recognized stream type in getNewestFrame()")
        }
        if (frame == null) {
            callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        } else {
            callback(frame.toBitmap())
        }
    }
}

class IllegalStreamTypeException(msg: String) : RuntimeException(msg)
