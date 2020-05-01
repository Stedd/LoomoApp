package com.example.loomoapp.OpenCV

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.Inference.MyInferenceKotlin
import com.example.loomoapp.Loomo.LoomoRealSense.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_WIDTH
import com.example.loomoapp.utils.NonBlockingInfLoop
import com.example.loomoapp.utils.RingBuffer
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core.gemm
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.dnn.Dnn
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_GRAY2RGBA
import org.opencv.imgproc.Imgproc.cvtColor


class OpenCVMain : Service() {
    private val TAG = "OpenCVMain"

    private lateinit var mLoaderCallback: BaseLoaderCallback
  //  lateinit var inference : MyInferenceKotlin

    init {
        //Load OpenCV

        if (!OpenCVLoader.initDebug()) {
            Log.d("$TAG init", "OpenCV not loaded")
        } else {
            Log.d("$TAG init", "OpenCV loaded")
        }

    }

    // Using a custom data class instead of the Pair-type/template for readability
    data class FrameData(val frame: Mat, val info: FrameInfo)

    private var fishEyeFrameBuffer = RingBuffer<FrameData>(30, true)
    private var colorFrameBuffer = RingBuffer<FrameData>(30, true)
    private var depthFrameBuffer = RingBuffer<FrameData>(30, true)
    private var newFishEyeFrames = 0
    private var newColorFrames = 0
    private var newDepthFrames = 0



    val fishEyeTracker = ORBTracker()
    var toggle = true


    val inference = MyInferenceKotlin()

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
        val tinyYoloCfg =  Environment.getExternalStorageDirectory().toString() + "dnns/yolov3-tiny-custom.cfg"
        val tinyYoloWight = Environment.getExternalStorageDirectory().toString() + "dnns/yolov3-tiny-custom_final.weights"
        inference.tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWight)

//        val m = Mat(5, 10, CV_8UC1, Scalar(0.0))
//        Log.d(TAG, "OpenCV Mat: $m")
//        val mr1: Mat = m.row(1)
//        mr1.setTo(Scalar(1.0))
//        val mc5: Mat = m.col(5)
//        mc5.setTo(Scalar(5.0))
//        Log.d(
//            TAG,
//            """OpenCV Mat data:
//                ${m.dump()}
//                """.trimIndent()
//        )
//        val src1 = Mat(3, 3, CV_64FC1, Scalar(2.5))
//        val src2 = Mat(3, 1, CV_64FC1, Scalar(2.0))
//        val src3 = Mat(3, 3, CV_64FC1, Scalar(1.0))
//        val dst = Mat()
//        gemm(src1, src2, 1.0, src3, 1.0, dst)
//        Log.d(TAG, """gemm result:
//            |${dst.dump()}
//        """.trimMargin())

    }

    var toggleOldState = toggle
    fun onNewFrame(streamType: Int, frame: Frame) {
//        val tic = System.currentTimeMillis()
        when (streamType) {
            StreamType.FISH_EYE -> {
                fishEyeFrameBuffer.enqueue(
                    FrameData(
                        frame.byteBuffer.toMat(
                            FISHEYE_WIDTH, FISHEYE_HEIGHT,
                            CV_8UC1
                        ), frame.info
                    )
                )
                ++newFishEyeFrames
            }
            StreamType.COLOR -> {
                colorFrameBuffer.enqueue(
                    FrameData(
                        frame.byteBuffer.toMat(
                            COLOR_WIDTH, COLOR_HEIGHT,
                            CV_8UC4
                        ), frame.info
                    )
                )
                ++newColorFrames
            }
            StreamType.DEPTH -> {
                depthFrameBuffer.enqueue(
                    FrameData(
                        frame.byteBuffer.toMat(
                            DEPTH_WIDTH, DEPTH_HEIGHT,
//                            CV_16UC1
                            CV_8UC2
                        ), frame.info
                    )
                )
                ++newDepthFrames
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
            StreamType.FISH_EYE -> {
                if (toggle) drawStuff(fishEyeFrame)
                else fishEyeFrameBuffer.peek().frame
            }
            StreamType.COLOR -> colorFrameBuffer.peek().frame
            StreamType.DEPTH -> depthFrameBuffer.peek().frame
            else -> throw IllegalStreamTypeException("Non recognized stream type in getNewestFrame()")
        }
        if (frame == null) {
            callback(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        } else {
            callback(frame.toBitmap())
        }
    }

    private var keyPoints = MatOfKeyPoint()
    private var pointPair = Pair<MatOfPoint2f, MatOfPoint2f>(MatOfPoint2f(), MatOfPoint2f())
    private var fishEyeFrame = Mat()
    private var processedFishEyeFrame = Mat()

    private val foo = NonBlockingInfLoop {
        inference.onFisheyeCameraFrame(fishEyeFrameBuffer.peek().frame)
//        if (newFishEyeFrames > 0) {
////            Log.d(TAG, "Skipped frames: ${newFishEyeFrames-1}")
//            newFishEyeFrames = 0
//            fishEyeFrame = fishEyeFrameBuffer.peek().frame
//            VisualOdometry.onNewFrame(fishEyeFrame)
//            if (toggleOldState != toggle) {
//                toggleOldState = toggle
//                Log.d(
//                    TAG,
//                    "Fisheye Mat() type: ${typeToString(fishEyeFrameBuffer.peek().frame.type())}"
//                )
//                Log.d(
//                    TAG,
//                    "Color Mat() type: ${typeToString(colorFrameBuffer.peek().frame.type())}"
//                )
//                Log.d(
//                    TAG,
//                    "Depth Mat() type: ${typeToString(depthFrameBuffer.peek().frame.type())}"
//                )
//            }
//            VisualOdometry.drawTrajectory().copyTo(map)
//            pointPair = fishEyeTracker.onNewFrame(fishEyeFrame)
//        }
////            Thread.sleep(2000) // Just for debugging purposes
    }

    val map = Mat.zeros(640, 480, CV_8UC3)
//    private val bar = NonBlockingInfLoop {
//        VisualOdometry.onNewFrame(fishEyeFrameBuffer.peek()!!.frame)
//        VisualOdometry.drawTrajectory().copyTo(map)
//    }


    private fun drawStuff(frame: Mat): Mat {
        val pointOld = pointPair.first.toArray()
        val point = pointPair.second.toArray()
        val img = Mat()
        frame.copyTo(img)
        if ((point.isNotEmpty()) and (pointOld.size == point.size) and !img.empty()) {
            cvtColor(img, img, COLOR_GRAY2RGBA)
//            Log.d(TAG, "Drawing lines")
            for (index in point.indices) {
                Imgproc.line(
                    img,
                    pointOld[index],
                    point[index],
                    Scalar(0.0, 0.0, 255.0, 127.0)
                )
                Imgproc.circle(
                    img,
                    pointOld[index],
                    3,
                    Scalar(0.0, 255.0, 0.0, 127.0)
                )
                Imgproc.circle(
                    img,
                    point[index],
                    3,
                    Scalar(255.0, 0.0, 255.0, 127.0)
                )
            }
        } else {
//            if (point.size != fishEyeTracker.numOfFeatures) {
//                Log.d(TAG, "point/n.o. feats. mismatch: ${point.size}/${fishEyeTracker.numOfFeatures}")
//            }
//            if (pointOld.size != point.size) {
//                Log.d(TAG, "Point-sets not equal size: pointOld(${pointOld.size}), point(${point.size})")
//            }
        }
//        val undistorted = Mat()
//        undistort(img, undistorted, fisheyeCameraMatrix, fisheyeDistortionCoefficients)
//        return undistorted
        return img
    }

//    fun yoloResults(result: Mat, outBlobNames: String) : (Mat, String) -> {
//
//    }

//    fun onCameraFrame(inputFrame: Mat) : Mat {
//        //var frame = inputFrame // få tilgang til fisheyeframe
//
//        var inferenceImage: Bitmap = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT,Bitmap.Config.ARGB_8888)
//        Log.d( TAG,"Bitmap");
//        inferenceImage.
//
//        // alt det andre som stod i funksjonen
//    }


}

class IllegalStreamTypeException(msg: String) : RuntimeException(msg)

//adb logcat | C:\Users\ja_ei\AppData\Local\Android\Sdk\ndk\21.0.6113669\ndk-stack -sym C:\OpenCVAndroidSDK\sdk\build\intermediates\cmake\debug\obj\x86_64