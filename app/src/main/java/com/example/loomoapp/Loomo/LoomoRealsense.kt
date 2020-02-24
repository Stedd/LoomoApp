package com.example.loomoapp.Loomo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.example.loomoapp.*
import com.example.loomoapp.ROS.ROSMain
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.stream.StreamType
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.imgproc.Imgproc

class LoomoRealsense {
    private val mVision :Vision = Vision.getInstance()


    private val imgWidthColor = 640
    private val imgHeightColor = 480
    private var mImgColor = Bitmap.createBitmap(
        imgWidthColor,
        imgHeightColor,
        Bitmap.Config.ARGB_8888
    )
    private var mImgColorResult = Bitmap.createBitmap(
        imgWidthColor,
        imgHeightColor,
        Bitmap.Config.ARGB_8888
    )

    private val imgWidthFishEye = 640
    private val imgHeightFishEye = 480
    private var mImgFishEye = Bitmap.createBitmap(
        imgWidthFishEye,
        imgHeightFishEye,
        Bitmap.Config.ALPHA_8
    )
    private var mImgFishEyeResult = Bitmap.createBitmap(
        imgWidthFishEye,
        imgHeightFishEye,
        Bitmap.Config.ARGB_8888
    )

    private val imgWidthDepth = 320
    private val imgHeightDepth = 240
    private var mImgDepth = Bitmap.createBitmap(
        imgWidthDepth,
        imgHeightDepth,
        Bitmap.Config.RGB_565
    )
    private var mImgDepthResult = Bitmap.createBitmap(
        imgWidthDepth,
        imgHeightDepth,
        Bitmap.Config.ARGB_8888
    )

    private var mImgDepthScaled =
        Bitmap.createScaledBitmap(mImgDepth, imgWidthColor / 3, imgWidthColor / 3, false)


    fun bind(context: Context){
        // Bind Vision SDK service
        mVision.bindService(context, object : ServiceBinder.BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Vision onBind ${mVision.isBind}")

                mROSMain.startConsumers()
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Vision unBind. Reason: $reason")
            }
        })
    }

    private fun startCamera(msg: String) {

        if (mVision.isBind) {
            if (!cameraRunning) {
                Log.i(TAG, msg)
                mVision.startListenFrame(StreamType.COLOR) { streamType, frame ->
                    mImgColor.copyPixelsFromBuffer(frame.byteBuffer)
//                    mImgColor = mImgFishEye.copy(Bitmap.Config.ARGB_8888, true)

                    //Convert to Mat
//                    Utils.bitmapToMat(mImgColor, imgFisheye)

                    //Canny edge detector
//                    Imgproc.Canny(imgFisheye, resultImgFisheye, 0.01, 190.0)

                    //ORB feature detector
//                    detectorColorCam.detect(imgFisheye, keypointsColorCam)
//                    Features2d.drawKeypoints(imgFisheye, keypointsColorCam, resultImgFisheye)

                    //Convert to Bitmap
//                    Utils.matToBitmap(resultImgFisheye, mImgColorResult)

                    //Scale result image
//                    mImgDepthScaled = Bitmap.createScaledBitmap(mImgColorResult, imgWidthColor/3, imgWidthColor/3,false)

                    //Show result image on main ui
                    threadHandler.post {
                        camView.setImageBitmap(mImgColorResult.copy(Bitmap.Config.ARGB_8888, true))
                    }
                }
                cameraRunning = true
            } else {
                Toast.makeText(this, "Dude, the camera is already activated..", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "Vision service not started yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera(msg: String) {
        if (cameraRunning) {
            Log.i(TAG, msg)
            mVision.stopListenFrame(StreamType.COLOR)
            cameraRunning = false
        }
        camView.setImageDrawable(getDrawable(R.drawable.ic_videocam))
    }


}