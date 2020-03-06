package com.example.loomoapp.ROS

import android.util.Log
import geometry_msgs.Twist
import nav_msgs.Odometry
import org.ros.message.MessageFactory
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.Node
import org.ros.node.topic.Publisher
import org.ros.node.topic.Subscriber
import org.ros.time.NtpTimeProvider
import sensor_msgs.CameraInfo
import sensor_msgs.CompressedImage
import sensor_msgs.Image
import sensor_msgs.Range
import std_msgs.Float32
import std_msgs.Int8
import tf2_msgs.TFMessage


class RosBridgeNode() :AbstractNodeMain() {

    private val TAG = "RosBridge"

    var RsDepthOpticalFrame = "rs_depth_optical_frame"
    var RsColorOpticalFrame = "rs_color_optical_frame"
    var FisheyeOpticalFrame = "fisheye_optical_frame"
    var UltrasonicFrame = "ultrasonic_frame"
    var LeftInfraredFrame = "infrared_left_frame"
    var RightInfraredFrame = "infrared_right_frame"
    var mConnectedNode: ConnectedNode? = null
    var mMessageFactory: MessageFactory? = null
    var mFisheyeCamPubr: Publisher<Image>? = null
    var mFisheyeCompressedPubr: Publisher<CompressedImage>? = null
    var mFisheyeCamInfoPubr: Publisher<CameraInfo>? = null
    var mRsColorPubr: Publisher<Image>? = null
    var mRsColorCompressedPubr: Publisher<CompressedImage>? = null
    var mRsDepthPubr: Publisher<Image>? = null
    var mRsColorInfoPubr: Publisher<CameraInfo>? = null
    lateinit var mRsDepthInfoPubr: Publisher<CameraInfo>
    var mTfPubr: Publisher<TFMessage>? = null
    var mInfraredPubrLeft: Publisher<Range>? = null
    var mInfraredPubrRight: Publisher<Range>? = null
    var mUltrasonicPubr: Publisher<Range>? = null
    var mBasePitchPubr: Publisher<Float32>? = null
    var mOdometryPubr: Publisher<Odometry>? = null
    var mTransformSubr: Subscriber<Int8>? = null
    var mCmdVelSubr: Subscriber<Twist>? = null
    var mNtpProvider: NtpTimeProvider? = null
    var node_name = "loomo_ros_bridge_node"
    var tf_prefix = "LO02"
    //TODO:Dynamic name
    var should_pub_ultrasonic = true
    var should_pub_infrared = true
    var should_pub_base_pitch = true
    var use_tf_prefix = true
    private val is_started = false
//    private val mOnStarted: Runnable
//    private val mOnShutdown: Runnable
    override fun onStart(connectedNode: ConnectedNode) {
        Log.d(TAG, "onStart() $connectedNode")
        super.onStart(connectedNode)
        Log.d(TAG, "onStart() creating publishers.")
        mConnectedNode = connectedNode
        mMessageFactory = connectedNode.topicMessageFactory
        if (use_tf_prefix == false) {
            tf_prefix = ""
        }
        if (use_tf_prefix) {
            RsDepthOpticalFrame = tf_prefix + "_" + RsDepthOpticalFrame
            RsColorOpticalFrame = tf_prefix + "_" + RsColorOpticalFrame
            FisheyeOpticalFrame = tf_prefix + "_" + FisheyeOpticalFrame
            UltrasonicFrame = tf_prefix + "_" + UltrasonicFrame
            LeftInfraredFrame = tf_prefix + "_" + LeftInfraredFrame
            RightInfraredFrame = tf_prefix + "_" + RightInfraredFrame
        }
        // Create publishers for many Loomo topics
        mFisheyeCamPubr = connectedNode.newPublisher(
            "$tf_prefix/fisheye/rgb/image",
            Image._TYPE
        )
        mFisheyeCompressedPubr = connectedNode.newPublisher(
            "$tf_prefix/fisheye/rgb/image/compressed",
            CompressedImage._TYPE
        )
        mFisheyeCamInfoPubr = connectedNode.newPublisher(
            "$tf_prefix/fisheye/rgb/camera_info",
            CameraInfo._TYPE
        )
        mRsColorPubr = connectedNode.newPublisher(
            "$tf_prefix/realsense_loomo/rgb/image",
            Image._TYPE
        )
        mRsColorCompressedPubr = connectedNode.newPublisher(
            "$tf_prefix/realsense_loomo/rgb/image/compressed",
            CompressedImage._TYPE
        )
        mRsColorInfoPubr = connectedNode.newPublisher(
            "$tf_prefix/realsense_loomo/rgb/camera_info",
            CameraInfo._TYPE
        )
        mRsDepthPubr = connectedNode.newPublisher(
            "$tf_prefix/realsense_loomo/depth/image",
            Image._TYPE
        )
        mRsDepthInfoPubr = connectedNode.newPublisher(
            "$tf_prefix/realsense_loomo/depth/camera_info",
            CameraInfo._TYPE
        )
        mTfPubr = connectedNode.newPublisher("/tf", TFMessage._TYPE)
        mInfraredPubrLeft = connectedNode.newPublisher(
            "$tf_prefix/left_infrared",
            Range._TYPE
        )
        mInfraredPubrRight = connectedNode.newPublisher(
            "$tf_prefix/right_infrared",
            Range._TYPE
        )
        mUltrasonicPubr = connectedNode.newPublisher(
            "$tf_prefix/ultrasonic",
            Range._TYPE
        )
        mBasePitchPubr = connectedNode.newPublisher(
            "$tf_prefix/base_pitch",
            Float32._TYPE
        )
        mOdometryPubr = connectedNode.newPublisher(
            "$tf_prefix/odom",
            Odometry._TYPE
        )
    }

    override fun onShutdown(node: Node) {
        super.onShutdown(node)
//        mOnShutdown.run()
    }

    override fun onShutdownComplete(node: Node) {
        super.onShutdownComplete(node)
    }

    override fun onError(node: Node, throwable: Throwable) {
        super.onError(node, throwable)
    }

    override fun getDefaultNodeName(): GraphName {
        if (use_tf_prefix) {
            node_name = "$tf_prefix/$node_name"
        }
        return GraphName.of(node_name)
    }

    companion object {
        private const val TAG = "RosBridgeNode"
    }

    init {
        //        this.mNtpProvider = ntpTimeProvider;
        Log.d(
            TAG,
            "Created instance of RosBridgeNode()."
        )
//        mOnStarted = onStarted
//        mOnShutdown = onShutdown
    }

}