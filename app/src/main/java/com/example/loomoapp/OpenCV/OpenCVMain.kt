package com.example.loomoapp.OpenCV

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.Loomo.LoomoRealSense
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.ROS.RealsensePublisher
import com.example.loomoapp.utils.RingBuffer
import com.example.loomoapp.utils.toByteArray
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.CvException
import org.opencv.core.CvType
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc.COLOR_BGR5652RGB
import org.opencv.imgproc.Imgproc.cvtColor
import java.nio.ByteBuffer

class OpenCVMain : Service() {
    private val TAG = "OpenCVMain"

    init {
        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("$TAG init", "OpenCV not loaded")
        } else {
            Log.d("$TAG init", "OpenCV loaded")
        }
    }

    private lateinit var mLoaderCallback: BaseLoaderCallback

    private var fishEyeFrame = Mat()
    private var colorFrame = Mat()
    private var depthFrame = Mat()

    private var fishEyeFrameBuffer = RingBuffer<Pair<Mat, FrameInfo>>(30, true)
    private var colorFrameBuffer = RingBuffer<Pair<Mat, FrameInfo>>(30, true)
    private var depthFrameBuffer = RingBuffer<Pair<Mat, FrameInfo>>(30, true)


    private val fishEyeTracker = ORBTracker()

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

    fun onNewFrame(streamType: Int, frame: Frame) {
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
                            CV_8UC2
                        ), frame.info
                    )
                )
            }
            else -> {
                throw IllegalStreamTypeException("Stream type not recognized in onNewFrame")
            }
        }
    }

    private fun ByteBuffer.toMat(width: Int, height: Int, cvType: Int): Mat {
        val mat = Mat()
        mat.create(height, width, cvType)
        mat.put(0, 0, this.toByteArray())
        return mat
    }

//    private fun writeToMain() {
//        mHandler.post {
//            mFishEyeByteBuffer.value = getFrame(fishEyeFrameBuffer.peek(0)!!)
//            mColorByteBuffer.value = getFrame(colorFrameBuffer.peek(0)!!)
//            mDepthByteBuffer.value = getFrame(depthFrameBuffer.peek(0)!!)
//        }
//    }

    //    fun newFishEyeFrame(byteBuf: ByteBuffer) {
//        newFrame(byteBuf, fishEyeFrame, FISHEYE_WIDTH, FISHEYE_HEIGHT, CV_8UC1)
//        fishEyeFrame = fishEyeTracker.onNewFrame(fishEyeFrame)
//    }
//    fun newColorFrame(byteBuf: ByteBuffer) {
//        newFrame(byteBuf, colorFrame, COLOR_WIDTH, COLOR_HEIGHT, CV_8UC4)
//    }
//    fun newDepthFrame(byteBuf: ByteBuffer) {
//        newFrame(byteBuf, depthFrame, DEPTH_WIDTH, DEPTH_HEIGHT, CV_8UC2)
//    }
//
//    fun getFishEyeFrame(): Bitmap {
//        return getFrame(fishEyeFrame)
//    }
//    fun getColorFrame(): Bitmap {
//        return getFrame(colorFrame)
//    }
//    fun getDepthFrame(): Bitmap {
//        return getFrame(depthFrame)
//    }
//
//    private fun newFrame(byteBuf: ByteBuffer, dst: Mat, width: Int, height: Int, cvType: Int) {
//        dst.create(height, width, cvType)
//        dst.put(0, 0, byteBuf.toByteArray())
//    }
//
    private fun Mat.toBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
        // matToBitmap() only works for CV_8UC1, CV_8UC3 and CV_8UC4 formats,
        // so the depth image's color space must be converted
        if (this.type() == CV_8UC2) {
            // The color space conversion for some reason does not change the frame.type(),
            // so the workaround is make a new 'Mat' with a usable type
            val tmp = Mat(DEPTH_HEIGHT, DEPTH_WIDTH, CV_8UC3)
            cvtColor(this, tmp, COLOR_BGR5652RGB)
            matToBitmap(tmp, bmp)
            return bmp
        }
//        if (!(frame.type() == CV_8UC1 || frame.type() == CV_8UC3 || frame.type() == CV_8UC4)) {
//        }
        if (this.empty()) {
            Log.d(TAG, "Frame is empty")
            return bmp
        }
        try {
            matToBitmap(this, bmp)
        } catch (e: CvException) {
            return bmp
        }
        return bmp
    }

    fun checkFrame(streamType: Int, callback: (Bitmap) -> Unit){
        val frame: Mat? = when(streamType) {
            StreamType.FISH_EYE -> fishEyeFrameBuffer.peekTail()?.first
            StreamType.COLOR -> colorFrameBuffer.peekTail()?.first
            StreamType.DEPTH -> depthFrameBuffer.peekTail()?.first
            else -> throw IllegalStreamTypeException("Non recognized stream type in observe()")
        }
        if (frame == null) {
            callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        } else {
            callback(frame.toBitmap())
        }
    }
}

class IllegalStreamTypeException(msg: String) : RuntimeException(msg)
