package com.example.loomoapp.Loomo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.*
import com.example.loomoapp.viewModel.MainActivityViewModel
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.stream.StreamType

class LoomoRealsense(viewModel: MainActivityViewModel) {
    private val TAG = "LoomoRealsense"
    private val mVision :Vision = Vision.getInstance()
    private val viewModel_ = viewModel

    private var cameraRunning = false

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
//                    mROSMain.startConsumers()
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Vision unBind. Reason: $reason")
            }
        })
    }

    fun startCamera(context: Context, msg: String) {
        if (mVision.isBind) {
            if (!cameraRunning) {
                Log.i(TAG, msg)
                // TODO: 25/02/2020 Separate individual cameras?
                mVision.startListenFrame(StreamType.COLOR) { streamType, frame ->
                    mImgColor.copyPixelsFromBuffer(frame.byteBuffer)
                    viewModel_.realSenseColorImage = MutableLiveData(mImgColor)
                }
                mVision.startListenFrame(StreamType.FISH_EYE) { streamType, frame ->
                    mImgFishEye.copyPixelsFromBuffer(frame.byteBuffer)
                    viewModel_.realSenseFishEyeImage = MutableLiveData(mImgFishEye)
                }
                mVision.startListenFrame(StreamType.DEPTH) { streamType, frame ->
                    mImgDepth.copyPixelsFromBuffer(frame.byteBuffer)
                    viewModel_.realSenseDepthImage = MutableLiveData(mImgDepth)
                }
                cameraRunning = true
            } else {
                Toast.makeText(context, "Dude, the camera is already activated..", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(context, "Vision service not started yet", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopCamera(context: Context, msg: String) {
        if (cameraRunning) {
            Log.i(TAG, msg)
            mVision.stopListenFrame(StreamType.COLOR)
            cameraRunning = false
        }
//        UIThreadHandler.post {
//            viewModel.image.setImageDrawable(getDrawable(context, R.drawable.ic_videocam))
//        }
    }


}