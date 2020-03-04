package com.example.loomoapp.Runnables

import android.util.Log
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.COLOR_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.DEPTH_WIDTH
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.ROS.RosBridgeNode
import com.segway.robot.sdk.vision.calibration.Intrinsic
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import org.jboss.netty.buffer.ChannelBufferOutputStream
import org.ros.node.topic.Publisher
import sensor_msgs.CameraInfo
import sensor_msgs.Image
import std_msgs.Header
import java.nio.ByteBuffer

class PublishNewFrame(
    private val source:Int,
    private val byteArray: ByteArray,
    private val info: FrameInfo,
    private val bridgeNode: RosBridgeNode,
    private var bufferStream: ChannelBufferOutputStream
) : Runnable {
    companion object {
        private const val TAG = "PublishNewFrame"
    }

    override fun run() {
        lateinit var image : Image
        bufferStream.write(byteArray)
        Log.d(TAG, "image data:${bufferStream.writtenBytes()}")
        when (source) {
            1 -> {
                //sensor_msgs/Image Message
                //https://docs.ros.org/melodic/api/sensor_msgs/html/msg/Image.html
                image = bridgeNode.mFisheyeCamPubr!!.newMessage()
                image.width = FISHEYE_WIDTH             //# image width, that is, number of columns
                image.height = FISHEYE_HEIGHT           //# image height, that is, number of rows
                image.step = FISHEYE_HEIGHT/8           //# Full row length in bytes
                image.encoding = "mono8"
                image.header.frameId = bridgeNode.FisheyeOpticalFrame

                image.data = bufferStream.buffer().copy()
//                Log.d(TAG, "Publishing FishEye camera frame: " + frame.info.frameNum);
                bridgeNode.mFisheyeCamPubr!!.publish(image)
            }
            2 -> {
                image = bridgeNode.mRsColorPubr!!.newMessage()
                image.width = COLOR_WIDTH
                image.height = COLOR_HEIGHT
                image.step = COLOR_HEIGHT/8
                image.encoding = "rgba8"
                image.header.frameId = bridgeNode.RsColorOpticalFrame

                image.data = bufferStream.buffer().copy()
//                Log.d(TAG, "Publishing color camera frame: " + frame.info.frameNum);
                bridgeNode.mRsColorPubr!!.publish(image)
            }
            3 -> {
                image = bridgeNode.mRsDepthPubr!!.newMessage()
                image.width = DEPTH_WIDTH
                image.height = DEPTH_HEIGHT
                image.step = DEPTH_HEIGHT/8
                image.encoding = "16u1"
                image.header.frameId = bridgeNode.RsDepthOpticalFrame

                image.data = bufferStream.buffer().copy()
//                Log.d(TAG, "Publishing Depth camera frame: " + frame.info.frameNum);
                bridgeNode.mRsDepthPubr!!.publish(image)
            }else -> {}
        }
        publishCameraInfo(source, image.header)
        bufferStream.buffer().clear()
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

