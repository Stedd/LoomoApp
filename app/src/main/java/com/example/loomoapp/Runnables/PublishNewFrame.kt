package com.example.loomoapp.Runnables

import android.graphics.Bitmap
import android.util.Log
import com.example.loomoapp.ROS.RosBridgeNode
import com.segway.robot.sdk.vision.calibration.Intrinsic
import com.segway.robot.sdk.vision.frame.Frame
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBufferOutputStream
import org.ros.node.topic.Publisher
import sensor_msgs.CameraInfo
import std_msgs.Header
import java.io.IOException
import java.nio.channels.Channels

class PublishNewFrame(

    private val frame: Frame,
    private val bridgeNode: RosBridgeNode,
    private val bufferStream: ChannelBufferOutputStream
) : Runnable {
    companion object {
        private const val TAG = "PublishNewFrame"
    }

//    private var tmpBitmap = camBitmap
    private val channel = Channels.newChannel(bufferStream)
    lateinit var aChannel: ChannelBuffer

    override fun run() {
//        Log.d(TAG, "New frame");

        val compressedImage = bridgeNode.mRsColorCompressedPubr!!.newMessage()
        val image = bridgeNode.mRsColorPubr!!.newMessage()
        compressedImage.format = "jpeg"
//        Log.d(TAG, "COLOR FRAME NUM: " + frame.info.frameNum);
//        Log.d(TAG, "COLOR FRAME PLATFORM STAMP: " + frame.info.platformTimeStamp);
        // If process_metadata doesn't want us to publish the frame, bail out now
//        if (!process_metadata(
//                RealsenseMetadataSource.COLOR,
//                frame.info,
//                compressedImage.header
//            )
//        ) {
//            Log.d(
//                TAG,
//                "WARNING: Skipping Color Frame " + frame.info.frameNum
//            )
////            return@FrameListener
//        }
        // Publish compressed image
//        compressedImage.header.frameId = bridgeNode.RsColorOpticalFrame
//        tmpBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferStream)
//        compressedImage.data = bufferStream.buffer().copy()
//        bridgeNode.mRsColorCompressedPubr!!.publish(compressedImage)
//        Log.d(TAG, "Publishing compressed color camera frame: " + frame.info.frameNum);
//        bufferStream.buffer().clear()

        // Publish raw image

        try {
            channel.write(frame.byteBuffer)
            Log.d(TAG, "writing buffer to channel:${frame.byteBuffer}. Channel: $channel" )

        } catch (e: IOException) {
            Log.e(TAG, "publishRsDepth: IO Exception:${e.message}")
        }
        image.data = bufferStream.buffer().copy()
        Log.d(TAG, "image data:${bufferStream.writtenBytes()}" )
        image.header.frameId = bridgeNode.RsColorOpticalFrame

        bridgeNode.mRsColorPubr!!.publish(image)
        Log.d(TAG, "Publishing color camera frame: " + frame.info.frameNum);
        bufferStream.buffer().clear()

        publishCameraInfo(2, compressedImage.header)
    }

    private fun publishCameraInfo(type: Int, header: Header) {
        val pubr: Publisher<CameraInfo>?
        val info: CameraInfo
        val intrinsic: Intrinsic
        val width: Int
        val height: Int
        // type: 1 for pcam, 2 for RsColor, 3 for RsDepth
        if (type == 1) { // Currently does not have camera info of platform camera
            Log.d(TAG, "publishCameraInfo type==1 -> not implemented.")
            return
        } else if (type == 2) {
            pubr = bridgeNode.mRsColorInfoPubr
//            intrinsic = mRsColorIntrinsic
            width = 640
            height = 480
        } else {
            pubr = bridgeNode.mRsDepthInfoPubr
//            intrinsic = mRsDepthIntrinsic
            width = 320
            height = 240
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

}
