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
import com.segway.robot.sdk.vision.frame.Frame
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

    private enum class RealsenseMetadataSource {
        DEPTH, COLOR, FISHEYE
    }

    private inner class RealsenseMetadata {
        var frameNum = 0
        var platformStamp: Long? = null
        var rosTime: Time? = null
        var source: RealsenseMetadataSource? = null
    }

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
    private var mRealsenseMeta: RealsenseMetadata? = null
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

//    fun publishFishEyeImage(frame: Frame) {
//        handlerThread.handler.post(
//            PublishNewFrame(
//                1,
//                frame,
//                mBridgeNode!!,
//                mFisheyeOutStream
//            )
//        )
//    }

    fun publishColorImage(byteBuffer: ByteBuffer, info:FrameInfo) {
        handlerThread.handler.post(
            PublishNewFrame(
                2,
                getByteBufferAsByteArray(byteBuffer),
                info,
                mBridgeNode!!,
                mRsColorOutStream
            )
        )
    }

//    fun publishDepthImage(frame: Frame) {
//        handlerThread.handler.post(
//            PublishNewFrame(
//                3,
//                frame,
//                mBridgeNode!!,
//                mRsDepthOutStream
//            )
//        )
//    }

    private fun getByteBufferAsByteArray(src: ByteBuffer): ByteArray {
        val bytesInBuffer = src.remaining()
        val tmpArr = ByteArray(bytesInBuffer) { src.get() }
//        src.rewind()
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

    @Synchronized
    private fun process_metadata(
        source: RealsenseMetadataSource,
        frameInfo: FrameInfo,
        imageHeader: Header
    ): Boolean {
        val currentFrame = frameInfo.frameNum
        val currentPlatformStamp = frameInfo.platformTimeStamp
        // Is there currently Realsense metadata?
        if (mRealsenseMeta != null) { // Did we generate this metadata?
            if (mRealsenseMeta!!.source == source) { // We generated this metadata, so a new frame has arrived before the other provider was able to
// consume it. Maybe a frame was dropped from the other camera.
// We should update the metadata here to match our current frame
                if (mRealsenseMeta!!.frameNum == currentFrame) {
                    Log.d(
                        TAG,
                        "ERROR: Camera callback for $source called twice for same frame!"
                    )
                    Log.d(
                        TAG,
                        "Current frame is $currentFrame but metadata contains frame $mRealsenseMeta"
                    )
                    return false
                    // TODO: this is fatal?
                } else if (mRealsenseMeta!!.frameNum > currentFrame) {
                    Log.d(
                        TAG,
                        "ERROR: Camera callback for $source called twice, with an old frame!"
                    )
                    Log.d(
                        TAG,
                        "Current frame is $currentFrame but metadata contains frame $mRealsenseMeta"
                    )
                    return false
                    // TODO: this is fatal?
                } else  // if (mRealsenseMeta.frameNum < currentFrame)
                {
                    Log.d(
                        TAG,
                        "WARNING: Camera callback for $source detected stale metadata: other source has fallen behind!"
                    )
                    Log.d(
                        TAG,
                        "Asked to process metadata for frame " + currentFrame + " but current metadata is for same source, frame " + mRealsenseMeta!!.frameNum
                    )
                    // Fall through to the end of the function where we generate new metadata
// TODO: is this the right choice? what should we do about this?
                }
            } else { // The other camera generated the metadata. We should validate it
// 3 possibilities:
// - The metadata matches our frame metadata exactly
// - Our metadata is newer
// - Our metadata is older
                when {
                    mRealsenseMeta!!.frameNum == currentFrame -> { // Consume the metadata and clear it
                        // Set the image header
                        imageHeader.stamp = mRealsenseMeta!!.rosTime
                        mRealsenseMeta = null
                        return true
                    }
                    mRealsenseMeta!!.frameNum > currentFrame -> { // We have an old frame, and should drop it to try and catch up
                        Log.d(
                            TAG,
                            "ERROR: Camera " + source + " has fallen behind. Processing frame num " + currentFrame + " but other source metadata has already processed " + mRealsenseMeta!!.frameNum
                        )
                        return false
                    }
                    else  // implied: if (mRealsenseMeta.frameNum < currentFrame)
                    -> { // Metadata from the other camera is old. This implies that the current source skipped a frame.
                        Log.d(
                            TAG,
                            "WARNING: Camera " + source + " is ahead. Processing frame num " + currentFrame + " but other source published metadata data for old frame " + mRealsenseMeta!!.frameNum
                        )
                        // We should create new metadata for the current frame. Fall through.
                        // TODO: is this the right choice?
                    }
                }
            }
        }
        // No metadata for this frame yet
// Get an appropriate ROS time to match the platform time of this stamp
        val currentRosTime = mBridgeNode!!.mConnectedNode!!.currentTime
        val currentSystemTime =
            Time.fromMillis(System.currentTimeMillis())
        val rosToSystemTimeOffset =
            currentRosTime.subtract(currentSystemTime)
        val stampTime =
            Time.fromNano(Utils.platformStampInNano(frameInfo.platformTimeStamp))
        val correctedStampTime = stampTime.add(rosToSystemTimeOffset)
        // Add the platform stamp / actual ROS time pair to the list of times we want TF data for
        mDepthRosStamps.add(
            Pair.create(
                frameInfo.platformTimeStamp,
                correctedStampTime
            )
        )
        // Create Realsense metadata for this frame
        mRealsenseMeta = RealsenseMetadata()
        mRealsenseMeta!!.source = source
        mRealsenseMeta!!.frameNum = currentFrame
        mRealsenseMeta!!.platformStamp = currentPlatformStamp
        mRealsenseMeta!!.rosTime = correctedStampTime
        // Set the image header
        imageHeader.stamp = correctedStampTime
        return true
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

    @Synchronized
    private fun publishCameraInfo(type: Int, header: Header) {
        val pubr: Publisher<CameraInfo>?
        val info: CameraInfo
        val intrinsic: Intrinsic
        val width: Int
        val height: Int
        // type: 1 for pcam, 2 for RsColor, 3 for RsDepth
        if (type == 1) { // Currently does not have camera info of platform camera
            Log.d(
                TAG,
                "publishCameraInfo type==1 -> not implemented."
            )
            return
        } else if (type == 2) {
            pubr = mBridgeNode!!.mRsColorInfoPubr
//            intrinsic = mRsColorIntrinsic
            width = COLOR_WIDTH
            height = COLOR_HEIGHT
        } else {
            pubr = mBridgeNode!!.mRsDepthInfoPubr
            intrinsic = mRsDepthIntrinsic
            width = DEPTH_WIDTH
            height = DEPTH_HEIGHT
        }
        info = pubr!!.newMessage()
        val k = DoubleArray(9)
        //        # Intrinsic camera matrix for the raw (distorted) images.
//        #     [fx  0 cx]
//        # K = [ 0 fy cy]
//        #     [ 0  0  1]
//        k[0] = intrinsic!!.focalLength.x.toDouble()
//        k[4] = intrinsic.focalLength.y.toDouble()
//        k[2] = intrinsic.principal.x.toDouble()
//        k[5] = intrinsic.principal.y.toDouble()
//        k[8] = 1.0
        k[0] = 1.0
        k[4] = 1.0
        k[2] = 1.0
        k[5] = 1.0
        k[8] = 1.0
        // # Projection/camera matrix
// #     [fx'  0  cx' Tx]
// # P = [ 0  fy' cy' Ty]
// #     [ 0   0   1   0]
        val p = DoubleArray(12)
//        p[0] = intrinsic.focalLength.x.toDouble()
//        p[5] = intrinsic.focalLength.y.toDouble()
//        p[2] = intrinsic.principal.x.toDouble()
//        p[6] = intrinsic.principal.y.toDouble()
//        p[10] = 1.0
        p[0] = 1.0
        p[5] = 1.0
        p[2] = 1.0
        p[6] = 1.0
        p[10] = 1.0
        // # Rectification matrix (stereo cameras only)
// # A rotation matrix aligning the camera coordinate system to the ideal
// # stereo image plane so that epipolar lines in both stereo images are
// # parallel.
        val r = DoubleArray(9)
        r[0] = 1.0
        r[4] = 1.0
        r[8] = 1.0
        info.header = header
        info.width = width
        info.height = height
        info.k = k
        info.p = p
        info.r = r
        pubr.publish(info)
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


