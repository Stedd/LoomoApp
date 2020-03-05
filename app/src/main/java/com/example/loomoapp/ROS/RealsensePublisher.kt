package com.example.loomoapp.ROS

import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import android.util.Log
import android.util.Pair
import com.example.loomoapp.LoopedThread
import com.example.loomoapp.Runnables.PublishNewFrame
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.calibration.Intrinsic
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.imu.IMUDataCallback
import org.jboss.netty.buffer.ChannelBufferOutputStream
import org.ros.internal.message.MessageBuffers
import org.ros.message.Time
import org.ros.node.topic.Publisher
import sensor_msgs.CameraInfo
import std_msgs.Header
import java.nio.ByteBuffer
import java.util.*

class RealsensePublisher(
    private val mDepthStamps: Queue<Long>,
    private val mDepthRosStamps: Queue<Pair<Long, Time>>,
    private val handlerThread: LoopedThread
) :
    RosBridge,
    IMUDataCallback {

    companion object {
        const val TAG = "RealsensePublisher"
    }

//    enum class RealsenseMetadataSource {
//        DEPTH, COLOR, FISHEYE
//    }
//
//    private inner class RealsenseMetadata {
//        var frameNum = 0
//        var platformStamp: Long? = null
//        var rosTime: Time? = null
//        var source: RealsenseMetadataSource? = null
//    }

    private var mBridgeNode: RosBridgeNode? = null
    private lateinit var vision_: Vision
    lateinit var mRsColorIntrinsic: Intrinsic
    lateinit var mRsDepthIntrinsic: Intrinsic
    lateinit var mFisheyeIntrinsic: Intrinsic
    private val mRsColorOutStream: ChannelBufferOutputStream =
        ChannelBufferOutputStream(MessageBuffers.dynamicBuffer()) //todo Hva er dette? Hva er forskjellen på dette og MutableLiveData?
    private val mRsDepthOutStream: ChannelBufferOutputStream =
        ChannelBufferOutputStream(MessageBuffers.dynamicBuffer())
    private val mFisheyeOutStream: ChannelBufferOutputStream =
        ChannelBufferOutputStream(MessageBuffers.dynamicBuffer())
//    private var mRealsenseMeta: RealsenseMetadata? = null
    private val mLatestDepthStamp = 0L

    override fun node_started(mBridgeNode: RosBridgeNode) {
        this.mBridgeNode = mBridgeNode
        Log.d(TAG, "RealsensePublisher node_started")
        start()
// TODO: 03/03/2020 Check what this is used for
//        start_imu()
    }

    fun setVision(vision: Vision) {
        vision_ = vision
    }

    override fun start() { // No generic initialization is required
        if (mBridgeNode == null || !vision_.isBind) {
            Log.d(TAG, "RealsensePublisher Cannot start , ROS or Loomo SDK is not ready")
            return
        } else {
            Log.d(TAG, "RealsensePublisher started")
        }

    }

    override fun stop() { // No generic de-initialization is required

    }

    // source: 1 for fisheye, 2 for Color, 3 for Depth

    fun publishFishEyeImage(byteArray: ByteArray, info:FrameInfo) {
        handlerThread.handler.post(
            PublishNewFrame(
                1,
                byteArray,
                info,
                mBridgeNode!!,
                mDepthRosStamps,
                mFisheyeOutStream
            )
        )
    }

    fun publishColorImage(byteArray: ByteArray, info:FrameInfo) {
        handlerThread.handler.post(PublishNewFrame(2,byteArray,info,mBridgeNode!!,mDepthRosStamps,mRsColorOutStream))
    }

    fun publishDepthImage(byteArray: ByteArray, info:FrameInfo) {
        handlerThread.handler.post(
            PublishNewFrame(
                3,
                byteArray,
                info,
                mBridgeNode!!,
                mDepthRosStamps,
                mRsDepthOutStream
            )
        )
    }

    private fun getByteBufferAsByteArray(src: ByteBuffer): ByteArray {
        val bytesInBuffer = src.remaining()
        val tmpArr = ByteArray(bytesInBuffer) { src.get() }
        src.rewind()
        return tmpArr
    }

    @Synchronized
    fun start_imu() {
        if (!vision_.isBind || mBridgeNode == null) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        vision_.setIMUCallback(this)
    }

    // TODO: 04/03/2020 Possible to Run config from menu on UI? Or at INIT. Calibration etc..
    fun updateCameraInfo(
        type: Int,
        ins: Intrinsic,
        width: Int,
        height: Int
    ) {
        if (type == 1) { // platform camera intrinsic not supported yet
            Log.w(
                TAG,
                "updateCameraInfo: platform camera intrinsic not supported yet!"
            )
        } else if (type == 2) {
            mRsColorIntrinsic = ins

        } else {
            mRsDepthIntrinsic = ins

        }
    }

    override fun onNewData(
        byteBuffer: ByteBuffer,
        length: Int,
        frameCount: Int
    ) { // TODO: what is this data
        Log.d(TAG, "Length: $length")
        Log.d(TAG, "Frame Count: $frameCount")
    }


}


