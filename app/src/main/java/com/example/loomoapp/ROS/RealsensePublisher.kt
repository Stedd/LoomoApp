package com.example.loomoapp.ROS

import android.graphics.Bitmap
import android.util.Log
import android.util.Pair
import com.example.loomoapp.Loomo.LoomoRealSense
import com.example.loomoapp.ROS.Utils.platformStampInNano
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.calibration.Intrinsic
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.imu.IMUDataCallback
import com.segway.robot.sdk.vision.stream.StreamType
import org.jboss.netty.buffer.ChannelBufferOutputStream
import org.ros.internal.message.MessageBuffers
import org.ros.message.Time
import org.ros.node.topic.Publisher
import sensor_msgs.CameraInfo
import std_msgs.Header
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.*

class RealsensePublisher(
    private val mDepthStamps: Queue<Long>,
    private val mDepthRosStamps: Queue<Pair<Long, Time>>,
    realSense: LoomoRealSense
) :
    RosBridge,
    IMUDataCallback {
    private enum class RealsenseMetadataSource {
        DEPTH, COLOR, FISHEYE
    }

    private inner class RealsenseMetadata {
        var frameNum = 0
        var platformStamp: Long? = null
        var rosTime: Time? = null
        var source: RealsenseMetadataSource? = null
    }

    val vision_ = realSense.mVision
    //    private var mVision: Vision? = null
    private var mBridgeNode: RosBridgeNode? = null
    lateinit var mRsColorIntrinsic: Intrinsic
    lateinit var mRsDepthIntrinsic: Intrinsic
    lateinit var mFisheyeIntrinsic: Intrinsic
    private var mRsColorWidth = 640
    private var mRsColorHeight = 480
    private var mRsDepthWidth = 320
    private var mRsDepthHeight = 240
    private val mFisheyeWidth = 640
    private val mFisheyeHeight = 480
    private val mRsColorOutStream: ChannelBufferOutputStream =
        ChannelBufferOutputStream(MessageBuffers.dynamicBuffer())
    private val mRsDepthOutStream: ChannelBufferOutputStream =
        ChannelBufferOutputStream(MessageBuffers.dynamicBuffer())
    private val mFisheyeOutStream: ChannelBufferOutputStream =
        ChannelBufferOutputStream(MessageBuffers.dynamicBuffer())
    private var mRealsenseMeta: RealsenseMetadata? = null
    private var mRsColorBitmap: Bitmap? = null
    private var mFisheyeBitmap: Bitmap? = null
    var mIsPubRsColor = false
    var mIsPubRsDepth = false
    var mIsPubFisheye = false
    private var mColorStarted = false
    private var mDepthStarted = false
    private var mFisheyeStarted = false
    private val mLatestDepthStamp = 0L

    override fun node_started(mBridgeNode: RosBridgeNode) {
        this.mBridgeNode = mBridgeNode
        Log.d(
            TAG,
            "RealsensePublisher node_started"
        )
    }

    override fun start() { // No generic initialization is required
        if (mBridgeNode == null || !vision_.isBind) {
            Log.d(
                TAG,
                "RealsensePublisher Cannot start , ROS or Loomo SDK is not ready"
            )
            return
        } else {
            Log.d(
                TAG,
                "RealsensePublisher started"
            )
        }
    }

    override fun stop() { // No generic de-initialization is required
// TODO: really?
    }

//    fun loomo_started(vision_: Vision) {
//        this.vision_ = vision_
//        // Get color-depth extrinsic and publish as a TF
//    }

    @Synchronized
    fun start_all() {
        if (!vision_.isBind || mBridgeNode == null) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        Log.d(TAG, "start_all() called")
        val infos = vision_.activatedStreamInfo
        for (info in infos) {
            when (info.streamType) {
                StreamType.COLOR -> {
                    updateCameraInfo(
                        2, vision_.colorDepthCalibrationData.colorIntrinsic,
                        info.width, info.height
                    )
                    vision_.startListenFrame(
                        StreamType.COLOR,
                        mRsColorListener
                    )
                }
                StreamType.DEPTH -> {
                    updateCameraInfo(
                        3, vision_.colorDepthCalibrationData.depthIntrinsic,
                        info.width, info.height
                    )
                    vision_.startListenFrame(
                        StreamType.DEPTH,
                        mRsDepthListener
                    )
                }
            }
        }
        Log.w(TAG, "start_all() done.")
    }

    @Synchronized
    fun stop_all() {
        if (!vision_.isBind || mBridgeNode == null) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        Log.d(TAG, "stop_all() called")
        val streamInfos = vision_.activatedStreamInfo
        for (info in streamInfos) {
            when (info.streamType) {
                StreamType.COLOR ->  // Stop color listener
                    vision_.stopListenFrame(StreamType.COLOR)
                StreamType.DEPTH ->  // Stop depth listener
                    vision_.stopListenFrame(StreamType.DEPTH)
            }
        }
        mColorStarted = false
        mDepthStarted = false
        mFisheyeStarted = false
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
    fun start_color() {
        if (!vision_.isBind || mBridgeNode == null || mColorStarted) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        mColorStarted = true
        Log.d(TAG, "start_color() called")
        updateCameraInfo(
            2, vision_.colorDepthCalibrationData.colorIntrinsic,
            mRsColorWidth, mRsColorHeight
        )
        vision_.startListenFrame(
            StreamType.COLOR,
            mRsColorListener
        )
    }

    @Synchronized
    fun start_depth() {
        if (!vision_.isBind || mBridgeNode == null || mDepthStarted) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        mDepthStarted = true
        Log.d(TAG, "start_depth() called")
        updateCameraInfo(
            3, vision_.colorDepthCalibrationData.depthIntrinsic,
            mRsDepthWidth, mRsDepthHeight
        )
        vision_.startListenFrame(
            StreamType.DEPTH,
            mRsDepthListener
        )
    }

    @Synchronized
    fun start_fisheye() {
        if (!vision_.isBind) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        mFisheyeStarted = true
        Log.d(TAG, "start_fisheye() called")
        //        updateCameraInfo(1, vision_.getColorDepthCalibrationData().colorIntrinsic,
//                mFisheyeWidth, mFisheyeHeight);
        vision_.startListenFrame(
            StreamType.FISH_EYE,
            mFisheyeListener
        )
    }

    @Synchronized
    fun stop_color() {
        if (!vision_.isBind || !mColorStarted) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        Log.d(TAG, "stop_color() called")
        vision_.stopListenFrame(StreamType.COLOR)
        mColorStarted = false
    }

    @Synchronized
    fun stop_depth() {
        if (!vision_.isBind || !mDepthStarted) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        Log.d(TAG, "stop_depth() called")
        vision_.stopListenFrame(StreamType.DEPTH)
        mDepthStarted = false
    }

    @Synchronized
    fun stop_fisheye() {
        if (!vision_.isBind || !mFisheyeStarted) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        Log.d(TAG, "stop_fisheye() called")
        vision_.stopListenFrame(StreamType.FISH_EYE)
        mFisheyeStarted = false
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

    var mRsColorListener =
        Vision.FrameListener { streamType, frame ->
            if (streamType != StreamType.COLOR) { //                TODO: this throws a lot of messages, why?
                //                Log.d(TAG, "mRsColorListener: !mIsPubRsColor");
                return@FrameListener
            }
            if (streamType != StreamType.COLOR) {
                Log.e(
                    TAG,
                    "onNewFrame@mRsColorListener: stream type not COLOR! THIS IS A BUG"
                )
                return@FrameListener
            }
            if (mRsColorBitmap == null || mRsColorBitmap!!.width != mRsColorWidth || mRsColorBitmap!!.height != mRsColorHeight
            ) {
                mRsColorBitmap =
                    Bitmap.createBitmap(mRsColorWidth, mRsColorHeight, Bitmap.Config.ARGB_8888)
            }
            val image = mBridgeNode!!.mRsColorCompressedPubr!!.newMessage()
            image.format = "jpeg"
            Log.d(TAG, "COLOR FRAME NUM: " + frame.getInfo().getFrameNum());
            //Log.d(TAG, "COLOR FRAME PLATFORM STAMP: " + frame.getInfo().getPlatformTimeStamp());
            // If process_metadata doesn't want us to publish the frame, bail out now
            if (!process_metadata(
                    RealsenseMetadataSource.COLOR,
                    frame.info,
                    image.header
                )
            ) {
                Log.d(
                    TAG,
                    "WARNING: Skipping Color Frame " + frame.info.frameNum
                )
                return@FrameListener
            }
            image.header.frameId = mBridgeNode!!.RsColorOpticalFrame
            // TODO: no more compression, it's too slow
            mRsColorBitmap!!.copyPixelsFromBuffer(frame.byteBuffer) // copy once
            mRsColorBitmap!!.compress(Bitmap.CompressFormat.JPEG, 75, mRsColorOutStream)
            image.data = mRsColorOutStream.buffer().copy() // copy twice
            mRsColorOutStream.buffer().clear()
            mBridgeNode!!.mRsColorCompressedPubr!!.publish(image)
            publishCameraInfo(2, image.header)
        }
    var mRsDepthListener =
        Vision.FrameListener { streamType, frame ->
            if (!mIsPubRsDepth) return@FrameListener
            if (streamType != StreamType.DEPTH) {
                Log.e(
                    TAG,
                    "onNewFrame@mRsDepthListener: stream type not DEPTH! THIS IS A BUG"
                )
                return@FrameListener
            }
            Log.d(TAG, "DEPTH FRAME NUM: " + frame.getInfo().getFrameNum());
            //Log.d(TAG, "DEPTH FRAME PLATFORM STAMP: " + frame.getInfo().getPlatformTimeStamp());
            val image = mBridgeNode!!.mRsDepthPubr!!.newMessage()
            image.width = mRsDepthWidth
            image.height = mRsDepthHeight
            image.step = mRsDepthWidth * 2
            image.encoding = "16UC1"
            image.header.frameId = mBridgeNode!!.RsDepthOpticalFrame
            // If process_metadata doesn't want us to publish the frame, bail out now
            if (!process_metadata(
                    RealsenseMetadataSource.DEPTH,
                    frame.info,
                    image.header
                )
            ) {
                Log.d(
                    TAG,
                    "WARNING: Skipping Depth Frame " + frame.info.frameNum
                )
                return@FrameListener
            }
            try {
                val channel =
                    Channels.newChannel(mRsDepthOutStream)
                channel.write(frame.byteBuffer)
            } catch (exception: IOException) {
                Log.e(
                    TAG,
                    String.format("publishRsDepth: IO Exception[%s]", exception.message)
                )
                return@FrameListener
            }
            image.data = mRsDepthOutStream.buffer().copy()
            mRsDepthOutStream.buffer().clear()
            mBridgeNode!!.mRsDepthPubr!!.publish(image)
            publishCameraInfo(3, image.header)
        }
    var mFisheyeListener =
        Vision.FrameListener { streamType, frame ->
            //            Log.d(TAG, "mRsColorListener onNewFrame...");
//            if (streamType != StreamType.FISH_EYE) {
//                Log.d(
//                    TAG,
//                    "mFisheyeListener: !mIsPubFisheye"
//                )
//                return@FrameListener
//            }
            if (streamType != StreamType.FISH_EYE) {
                Log.e(
                    TAG,
                    "onNewFrame@mFisheyeListener: stream type not FISH_EYE! THIS IS A BUG"
                )
                return@FrameListener
            }
            if (mFisheyeBitmap == null || mFisheyeBitmap!!.width != mFisheyeWidth || mFisheyeBitmap!!.height != mFisheyeHeight
            ) {
                mFisheyeBitmap =
                    Bitmap.createBitmap(mFisheyeWidth, mFisheyeHeight, Bitmap.Config.ALPHA_8)
            }
            mFisheyeBitmap!!.copyPixelsFromBuffer(frame.byteBuffer) // copy once
            val image = mBridgeNode!!.mFisheyeCompressedPubr!!.newMessage()
            image.format = "jpeg"
            image.header.stamp = Time.fromNano(platformStampInNano(frame.info.platformTimeStamp))
            image.header.frameId = mBridgeNode!!.FisheyeOpticalFrame
            mFisheyeBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, mFisheyeOutStream)
            image.data = mFisheyeOutStream.buffer().copy() // copy twice
            mFisheyeOutStream.buffer().clear()
            mBridgeNode!!.mFisheyeCompressedPubr!!.publish(image)
            //            publishCameraInfo(2, image.getHeader());
        }

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
            mRsColorWidth = width
            mRsColorHeight = height
        } else {
            mRsDepthIntrinsic = ins
            mRsDepthWidth = width
            mRsDepthHeight = height
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
            intrinsic = mRsColorIntrinsic
            width = mRsColorWidth
            height = mRsColorHeight
        } else {
            pubr = mBridgeNode!!.mRsDepthInfoPubr
            intrinsic = mRsDepthIntrinsic
            width = mRsDepthWidth
            height = mRsDepthHeight
        }
        info = pubr!!.newMessage()
        val k = DoubleArray(9)
        //        # Intrinsic camera matrix for the raw (distorted) images.
//        #     [fx  0 cx]
//        # K = [ 0 fy cy]
//        #     [ 0  0  1]
        k[0] = intrinsic!!.focalLength.x.toDouble()
        k[4] = intrinsic.focalLength.y.toDouble()
        k[2] = intrinsic.principal.x.toDouble()
        k[5] = intrinsic.principal.y.toDouble()
        k[8] = 1.0
        // # Projection/camera matrix
// #     [fx'  0  cx' Tx]
// # P = [ 0  fy' cy' Ty]
// #     [ 0   0   1   0]
        val p = DoubleArray(12)
        p[0] = intrinsic.focalLength.x.toDouble()
        p[5] = intrinsic.focalLength.y.toDouble()
        p[2] = intrinsic.principal.x.toDouble()
        p[6] = intrinsic.principal.y.toDouble()
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

    companion object {
        const val TAG = "RealsensePublisher"
    }

}