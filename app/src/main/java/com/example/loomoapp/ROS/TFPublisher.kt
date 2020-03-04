package com.example.loomoapp.ROS

import android.util.Log
import android.util.Pair
import com.example.loomoapp.Loomo.LoomoBase
import com.example.loomoapp.Loomo.LoomoRealSense
import com.example.loomoapp.Loomo.LoomoSensor
import com.example.loomoapp.LoopedThread
import com.segway.robot.algo.tf.AlgoTfData
import com.segway.robot.sdk.locomotion.sbv.AngularVelocity
import com.segway.robot.sdk.locomotion.sbv.LinearVelocity
import com.segway.robot.sdk.perception.sensor.Sensor
import com.segway.robot.sdk.vision.calibration.ColorDepthCalibration
import com.segway.robot.sdk.vision.calibration.Extrinsic
import com.segway.robot.sdk.vision.calibration.MotionModuleCalibration
import geometry_msgs.Quaternion
import geometry_msgs.Transform
import geometry_msgs.TransformStamped
import geometry_msgs.Vector3
import nav_msgs.Odometry
import org.ros.message.Duration
import org.ros.message.Time
import java.util.*

class TFPublisher(
    private val mDepthStamps: Queue<Long>,
    private val mDepthRosStamps: Queue<Pair<Long, Time>>?,
    base_: LoomoBase,
    sensor_: LoomoSensor,
    realSense_: LoomoRealSense,
    private val handlerThread: LoopedThread
) : RosBridge {

    companion object {
        const val TAG = "TFPublisher"
    }

    private val base = base_.mBase
    private val sensor = sensor_.mSensor
    private val vision = realSense_.mVision

    var mIsPubTF = false

    //    private var mTFPublishThread: Thread = TFPublisherThread()
    private var mBridgeNode: RosBridgeNode? = null
    lateinit var mDepthCalibration: ColorDepthCalibration
    lateinit var mMotionCalibration: MotionModuleCalibration
    private var mStarted = false

    override fun node_started(mBridgeNode: RosBridgeNode) {
        this.mBridgeNode = mBridgeNode
        Log.d(TAG, "TFPublisher node_started")
        start()
    }

    override fun start() {
        if (mStarted || !vision.isBind) {
            Log.d(TAG, "TFPublisher Cannot start_listening yet, a required service is not ready")
            return
        } else {
            Log.d(TAG, "TFPublisher started")
        }
        mStarted = true
        mDepthCalibration = vision.colorDepthCalibrationData
            Log.d(TAG, "DepthCalibration $mDepthCalibration")
        mMotionCalibration = vision.motionModuleCalibrationData
            Log.d(TAG, "MotionCalibration $mMotionCalibration")
        Log.d(
            TAG,
            "Depth extrinsic data: " + mDepthCalibration.depthToColorExtrinsic.toString()
        )
        Log.d(TAG, "start_tf()")

//        mTFPublishThread.start()
    }

    override fun stop() {
        if (!mStarted) {
            return
        }
        Log.d(TAG, "stop_tf()")
        try {
//            mTFPublishThread.join()
        } catch (e: InterruptedException) {
            Log.w(
                TAG,
                "onUnbind: mSensorPublishThread.join() ",
                e
            )
        }
        mStarted = false
    }

    fun publishTF() {
//        handlerThread.handler.post(PublishTFMessage(mDepthRosStamps!!.poll()))
    }

    fun publishOdometry() {
        Log.d(TAG, "Stamp:${mDepthRosStamps} ");
//        handlerThread.handler.post(PublishOdometry(mDepthRosStamps!!.poll()))
    }

    fun updateTimestamp(){
        // TODO: 04/03/2020 This needs the time of a camera frame?
//        val currentRosTime = mBridgeNode!!.mConnectedNode!!.currentTime
//        val currentSystemTime = Time.fromMillis(System.currentTimeMillis())
//        val rosToSystemTimeOffset = currentRosTime.subtract(currentSystemTime)
//        val stampTime = Time.fromNano(Utils.platformStampInNano(frameInfo.platformTimeStamp))
//        val correctedStampTime = stampTime.add(rosToSystemTimeOffset)
//        // Add the platform stamp / actual ROS time pair to the list of times we want TF data for
//        mDepthRosStamps?.add(
//            Pair.create(
//                frameInfo.platformTimeStamp,
//                correctedStampTime
//            )
//        )
    }


    private inner class PublishOdometry(val stamp: Pair<Long, Time>) : Runnable {
        override fun run() {
            val tfData = sensor.getTfData(
                Sensor.BASE_POSE_FRAME,
                Sensor.WORLD_ODOM_ORIGIN,
                stamp.first,
                100
            )
//            val linearVelocity = base.linearVelocity
//            val angularVelocity = base.angularVelocity
            val odom_message = produceOdometryMessage(
                tfData,
                base.linearVelocity,
                base.angularVelocity,
                stamp.second
            )
            mBridgeNode!!.mOdometryPubr!!.publish(odom_message)
            Log.d(TAG, odom_message.toString())
        }
    }

    val frameNames =
        listOf(
            Sensor.WORLD_ODOM_ORIGIN,  // 0, odom
            Sensor.BASE_POSE_FRAME,  // 1, base_link
            Sensor.NECK_POSE_FRAME,  // 2, neck_link
            Sensor.HEAD_POSE_Y_FRAME,  // 3, head_link
            Sensor.RS_COLOR_FRAME,  // 4, rs_color
            Sensor.RS_DEPTH_FRAME,  // 5, rs_depth
            Sensor.HEAD_POSE_P_R_FRAME,  // 6, tablet_link
            Sensor.PLATFORM_CAM_FRAME
        ) // 7, plat_cam_link
    val frameIndices =
        listOf(
            Pair(0, 1),  // odom to base_link
            Pair(1, 2),  // base_link to neck_link
            Pair(2, 3),  // neck_link to head_link
            Pair(3, 4),  // head_link to rs_color
            Pair(3, 5),  // head_link to rs_depth
            Pair(3, 6),  // head_link to tablet_link
            Pair(6, 7)
        ) // tablet_link to plat_cam_link

    private inner class PublishTFMessage(val stamp: Pair<Long, Time>) : Runnable {

        override fun run() {

            val tfMessage = mBridgeNode!!.mTfPubr!!.newMessage()
            for (index in frameIndices) {
                var target = frameNames[index.second]
                var source = frameNames[index.first]
                // Swapped source/target because it seemed backwards in RViz
                val tfData = sensor.getTfData(target, source, stamp.first, 100)
                //Log.d(TAG, tfData.toString());
// ROS usually uses "base_link" and "odom" as fundamental tf names
// definitely could remove this if you prefer Loomo's names
                if (source == Sensor.BASE_POSE_FRAME) {
                    source = "base_link"
                }
                if (target == Sensor.BASE_POSE_FRAME) {
                    target = "base_link"
                }
                if (source == Sensor.WORLD_ODOM_ORIGIN) {
                    source = "odom"
                }
                if (target == Sensor.WORLD_ODOM_ORIGIN) {
                    target = "odom"
                }
                // Add tf_prefix to each transform before ROS publishing (in case of multiple loomos on one network)
                if (mBridgeNode!!.use_tf_prefix) {
                    tfData.srcFrameID = mBridgeNode!!.tf_prefix + "_" + source
                    tfData.tgtFrameID = mBridgeNode!!.tf_prefix + "_" + target
                }
                if (stamp.first != tfData.timeStamp) {
                    Log.d(
                        TAG, String.format(
                            "ERROR: getTfData failed for frames[%d]: %s -> %s",
                            stamp.first, source, target
                        )
                    )
                    continue
                }
                val transformStamped =
                    algoTf2TfStamped(tfData, stamp.second)
                tfMessage.transforms.add(transformStamped)
            }
            // Publish the Sensor.RS_COLOR_FRAME -> RsOpticalFrame transform
            val loomoToRsCameraTf =
                realsenseColorToOpticalFrame(stamp.second)
            tfMessage.transforms.add(loomoToRsCameraTf)
            // Publish the RsOpticalFrame -> RsDepthFrame transform
// TODO: compute statically and just update the timestamp
            val rsColorToDepthTf = realsenseColorToDepthExtrinsic(
                mDepthCalibration.depthToColorExtrinsic,
                stamp.second
            )
            tfMessage.transforms.add(rsColorToDepthTf)
            // Publish the base_link -> ultrasonic frame transform
// Ultrasonic is static TF, 44cm up, 12cm forward
            val ultrasonicTf = baseLinkToUltrasonicTransform(stamp.second)
            tfMessage.transforms.add(ultrasonicTf)
            if (tfMessage.transforms.size > 0) {
                mBridgeNode!!.mTfPubr!!.publish(tfMessage)
            }

        }
    }


    private inner class TFPublisherThread : Thread() {
        override fun run() {
            Log.d(TAG, "run: SensorPublisherThread")
            super.run()
            // New TF tree:
// Sensor.WORLD_ODOM_ORIGIN
//  | Sensor.BASE_POSE_FRAME                <- base_link, the co-ordinate frame relative to the robot platform
//     | Sensor.NECK_POSE_FRAME             <- neck_link, the co-ordinate frame relative to the neck attached to the base, not moving
//        | Sensor.HEAD_POSE_Y_FRAME        <- neck_link_yaw, the co-ordinate frame of the head. O translation, but incorporates the yaw of the head
//           | Sensor.RS_COLOR_FRAME
//           | Sensor.RS_DEPTH_FRAME
//           | Sensor.HEAD_POSE_P_R_FRAME
//              | Sensor.PLATFORM_CAM_FRAME
            val frameNames =
                listOf(
                    Sensor.WORLD_ODOM_ORIGIN,  // 0, odom
                    Sensor.BASE_POSE_FRAME,  // 1, base_link
                    Sensor.NECK_POSE_FRAME,  // 2, neck_link
                    Sensor.HEAD_POSE_Y_FRAME,  // 3, head_link
                    Sensor.RS_COLOR_FRAME,  // 4, rs_color
                    Sensor.RS_DEPTH_FRAME,  // 5, rs_depth
                    Sensor.HEAD_POSE_P_R_FRAME,  // 6, tablet_link
                    Sensor.PLATFORM_CAM_FRAME
                ) // 7, plat_cam_link
            val frameIndices =
                listOf(
                    Pair(0, 1),  // odom to base_link
                    Pair(1, 2),  // base_link to neck_link
                    Pair(2, 3),  // neck_link to head_link
                    Pair(3, 4),  // head_link to rs_color
                    Pair(3, 5),  // head_link to rs_depth
                    Pair(3, 6),  // head_link to tablet_link
                    Pair(6, 7)
                ) // tablet_link to plat_cam_link
            Log.d(TAG, "TFpublisher before while");
            while (sensor.isBind) {
//                Log.d(TAG, "TFpublisher insite while $mDepthRosStamps");
                // TODO: 26/02/2020 This condition is not true
                if (mDepthRosStamps == null) {
                    Log.d(TAG, "TFpublisher mDepthRosStamps continue");
                    continue
                }
                val stamp =
                    mDepthRosStamps.poll()
                if (stamp != null) { // Get an appropriate ROS time to match the platform time of this stamp
                    Log.d(TAG, "TFpublisher Run:");
/*
                    Time currentRosTime = mBridgeNode!!.mConnectedNode.getCurrentTime();
                    Time currentSystemTime = Time.fromMillis(System.currentTimeMillis());
                    Time stampTime = Time.fromNano(Utils.platformStampInNano(stamp));
                    Duration rosToSystemTimeOffset = currentRosTime.subtract(currentSystemTime);
                    Time correctedStampTime = stampTime.add(rosToSystemTimeOffset);

                    Log.d(TAG, "node: " + currentRosTime.toString());
                    Log.d(TAG, "sys: " + currentSystemTime.toString());
                    Log.d(TAG, "node-sys diff: " + rosToSystemTimeOffset.toString());

                    Log.d(TAG, "node: " + currentRosTime.toString());
                    Log.d(TAG, "stamp: " + stampTime.toString());
                    Log.d(TAG, "node-stamp diff: " + (currentRosTime.subtract(stampTime)).toString());
                    Log.d(TAG, "True stamp: " + correctedStampTime.toString());
                    Log.d(TAG, "True node-stamp diff: " + (currentRosTime.subtract(correctedStampTime)).toString());
                    */
                    val tfMessage = mBridgeNode!!.mTfPubr!!.newMessage()
                    for (index in frameIndices) {
                        var target = frameNames[index.second]
                        var source = frameNames[index.first]
                        // Swapped source/target because it seemed backwards in RViz
                        val tfData = sensor.getTfData(target, source, stamp.first, 100)
                        //Log.d(TAG, tfData.toString());
// ROS usually uses "base_link" and "odom" as fundamental tf names
// definitely could remove this if you prefer Loomo's names
                        if (source == Sensor.BASE_POSE_FRAME) {
                            source = "base_link"
                        }
                        if (target == Sensor.BASE_POSE_FRAME) {
                            target = "base_link"
                        }
                        if (source == Sensor.WORLD_ODOM_ORIGIN) {
                            source = "odom"
                        }
                        if (target == Sensor.WORLD_ODOM_ORIGIN) {
                            target = "odom"
                        }
                        // Add tf_prefix to each transform before ROS publishing (in case of multiple loomos on one network)
                        if (mBridgeNode!!.use_tf_prefix) {
                            tfData.srcFrameID = mBridgeNode!!.tf_prefix + "_" + source
                            tfData.tgtFrameID = mBridgeNode!!.tf_prefix + "_" + target
                        }
                        if (stamp.first != tfData.timeStamp) {
                            Log.d(
                                TAG, String.format(
                                    "ERROR: getTfData failed for frames[%d]: %s -> %s",
                                    stamp.first, source, target
                                )
                            )
                            continue
                        }
                        val transformStamped =
                            algoTf2TfStamped(tfData, stamp.second)
                        tfMessage.transforms.add(transformStamped)
                    }
                    // Publish the Sensor.RS_COLOR_FRAME -> RsOpticalFrame transform
                    val loomoToRsCameraTf =
                        realsenseColorToOpticalFrame(stamp.second)
                    tfMessage.transforms.add(loomoToRsCameraTf)
                    // Publish the RsOpticalFrame -> RsDepthFrame transform
// TODO: compute statically and just update the timestamp
                    val rsColorToDepthTf = realsenseColorToDepthExtrinsic(
                        mDepthCalibration.depthToColorExtrinsic,
                        stamp.second
                    )
                    tfMessage.transforms.add(rsColorToDepthTf)
                    // Publish the base_link -> ultrasonic frame transform
// Ultrasonic is static TF, 44cm up, 12cm forward
                    val ultrasonicTf = baseLinkToUltrasonicTransform(stamp.second)
                    tfMessage.transforms.add(ultrasonicTf)
                    if (tfMessage.transforms.size > 0) {
                        mBridgeNode!!.mTfPubr!!.publish(tfMessage)
                    }
                    // Swapped source/target because it seemed backwards in RViz
// TODO: this isn't capturing the velocity at the sensor timestamp. Consider moving to another thread to publish this as fast as possible
//                    val tfData = sensor.getTfData(
//                        Sensor.BASE_POSE_FRAME,
//                        Sensor.WORLD_ODOM_ORIGIN,
//                        stamp.first,
//                        100
//                    )
//                    val linearVelocity = base.linearVelocity
//                    val angularVelocity = base.angularVelocity
//                    val odom_message = produceOdometryMessage(
//                        tfData,
//                        linearVelocity,
//                        angularVelocity,
//                        stamp.second
//                    )
//                    mBridgeNode!!.mOdometryPubr!!.publish(odom_message)
//                    Log.d(TAG, odom_message.toString())
                }
            }
            // TODO: 26/02/2020 test this
//            sleep(50)
        }

    }

    private fun realsenseColorToDepthExtrinsic(
        extrinsic: Extrinsic,
        time: Time
    ): TransformStamped {
        val vector3: Vector3 =
            mBridgeNode!!.mMessageFactory!!.newFromType(Vector3._TYPE)
        // Extrinsic data is in mm, convert to meters
        vector3.x = extrinsic.translation.x / 1000.0f.toDouble()
        vector3.y = extrinsic.translation.y / 1000.0f.toDouble()
        vector3.z = extrinsic.translation.z / 1000.0f.toDouble()
        val quaternion: Quaternion =
            mBridgeNode!!.mMessageFactory!!.newFromType(Quaternion._TYPE)
        val identity_quaternion =
            org.ros.rosjava_geometry.Quaternion.identity()
        quaternion.x = identity_quaternion.x
        quaternion.y = identity_quaternion.y
        quaternion.z = identity_quaternion.z
        quaternion.w = identity_quaternion.w
        val transform: Transform =
            mBridgeNode!!.mMessageFactory!!.newFromType(Transform._TYPE)
        transform.translation = vector3
        transform.rotation = quaternion
        val transformStamped: TransformStamped = mBridgeNode!!.mMessageFactory!!.newFromType(
            TransformStamped._TYPE
        )
        transformStamped.transform = transform
        transformStamped.childFrameId = mBridgeNode!!.RsDepthOpticalFrame
        transformStamped.header.frameId = mBridgeNode!!.RsColorOpticalFrame
        // Future-date this static transform so that depth_image_proc/register can get the TF easier
        transformStamped.header.stamp = time.add(Duration.fromMillis(1000))
        return transformStamped
    }

    private fun realsenseColorToOpticalFrame(time: Time): TransformStamped {
        val vector3: Vector3 =
            mBridgeNode!!.mMessageFactory!!.newFromType(Vector3._TYPE)
        // Assume that rscolor_optical_frame is in the same physical location as Sensor.RS_COLOR_FRAME, but with a different rotation
        vector3.x = 0.0
        vector3.y = 0.0
        vector3.z = 0.0
        // Rotate the vector around the X axis
        var camera_rotation_x =
            org.ros.rosjava_geometry.Quaternion.fromAxisAngle(
                org.ros.rosjava_geometry.Vector3.xAxis(),
                Math.PI / -2.0
            )
        val camera_rotation_y =
            org.ros.rosjava_geometry.Quaternion.fromAxisAngle(
                org.ros.rosjava_geometry.Vector3.yAxis(),
                Math.PI / 2.0
            )
        val camera_rotation_z =
            org.ros.rosjava_geometry.Quaternion.fromAxisAngle(
                org.ros.rosjava_geometry.Vector3.zAxis(),
                Math.PI / 2.0
            )
        camera_rotation_x = camera_rotation_x.multiply(camera_rotation_y)
        //camera_rotation_x = camera_rotation_x.multiply(camera_rotation_z);
        val quaternion: Quaternion =
            mBridgeNode!!.mMessageFactory!!.newFromType(Quaternion._TYPE)
        quaternion.x = camera_rotation_x.x
        quaternion.y = camera_rotation_x.y
        quaternion.z = camera_rotation_x.z
        quaternion.w = camera_rotation_x.w
        val transform: Transform =
            mBridgeNode!!.mMessageFactory!!.newFromType(Transform._TYPE)
        transform.translation = vector3
        transform.rotation = quaternion
        val transformStamped: TransformStamped = mBridgeNode!!.mMessageFactory!!.newFromType(
            TransformStamped._TYPE
        )
        transformStamped.transform = transform
        transformStamped.childFrameId = mBridgeNode!!.RsColorOpticalFrame
        transformStamped.header.frameId = mBridgeNode!!.tf_prefix + "_" + Sensor.RS_COLOR_FRAME
        transformStamped.header.stamp = time
        return transformStamped
    }

    private fun staticFullTransform(
        t_x: Float,
        t_y: Float,
        t_z: Float,
        q_x: Float,
        q_y: Float,
        q_z: Float,
        q_w: Float
    ): Transform {
        val vector3: Vector3 =
            mBridgeNode!!.mMessageFactory!!.newFromType(Vector3._TYPE)
        vector3.x = t_x.toDouble()
        vector3.y = t_y.toDouble()
        vector3.z = t_z.toDouble()
        val quaternion: Quaternion =
            mBridgeNode!!.mMessageFactory!!.newFromType(Quaternion._TYPE)
        quaternion.x = q_x.toDouble()
        quaternion.y = q_y.toDouble()
        quaternion.z = q_z.toDouble()
        quaternion.w = q_w.toDouble()
        val transform: Transform =
            mBridgeNode!!.mMessageFactory!!.newFromType(Transform._TYPE)
        transform.translation = vector3
        transform.rotation = quaternion
        return transform
    }

    private fun staticTranslationTransform(
        x: Float,
        y: Float,
        z: Float
    ): Transform {
        val identity_quaternion =
            org.ros.rosjava_geometry.Quaternion.identity()
        return staticFullTransform(
            x,
            y,
            z,
            identity_quaternion.x.toFloat(),
            identity_quaternion.y.toFloat(),
            identity_quaternion.z.toFloat(),
            identity_quaternion.w.toFloat()
        )
    }

    private fun baseLinkToNeckTransform(time: Time): TransformStamped {
        val transformStamped: TransformStamped = mBridgeNode!!.mMessageFactory!!.newFromType(
            TransformStamped._TYPE
        )
        // Neck is approx 50cm above the base
        transformStamped.transform = staticTranslationTransform(0f, 0f, 0.5f)
        var sourceFrame =
            Sensor.BASE_ODOM_FRAME
        var targetFrame =
            Sensor.NECK_POSE_FRAME
        if (mBridgeNode!!.use_tf_prefix) {
            sourceFrame = mBridgeNode!!.tf_prefix + "_" + sourceFrame
            targetFrame = mBridgeNode!!.tf_prefix + "_" + targetFrame
        }
        transformStamped.header.frameId = sourceFrame
        transformStamped.childFrameId = targetFrame
        // Future-date this static transform
        transformStamped.header.stamp = time.add(Duration.fromMillis(100))
        return transformStamped
    }

    private fun baseLinkToUltrasonicTransform(time: Time): TransformStamped {
        val vector3: Vector3 =
            mBridgeNode!!.mMessageFactory!!.newFromType(Vector3._TYPE)
        // 44cm = 0.44m
// 12cm = 0.12m
        vector3.x = 0.12
        vector3.y = 0.0
        vector3.z = 0.44
        val quaternion: Quaternion =
            mBridgeNode!!.mMessageFactory!!.newFromType(Quaternion._TYPE)
        val identity_quaternion =
            org.ros.rosjava_geometry.Quaternion.identity()
        quaternion.x = identity_quaternion.x
        quaternion.y = identity_quaternion.y
        quaternion.z = identity_quaternion.z
        quaternion.w = identity_quaternion.w
        val transform: Transform =
            mBridgeNode!!.mMessageFactory!!.newFromType(Transform._TYPE)
        transform.translation = vector3
        transform.rotation = quaternion
        val transformStamped: TransformStamped = mBridgeNode!!.mMessageFactory!!.newFromType(
            TransformStamped._TYPE
        )
        transformStamped.transform = transform
        transformStamped.childFrameId = mBridgeNode!!.UltrasonicFrame
        var sourceFrame = "base_link"
        if (mBridgeNode!!.use_tf_prefix) {
            sourceFrame = mBridgeNode!!.tf_prefix + "_" + sourceFrame
        }
        transformStamped.header.frameId = sourceFrame
        // Future-date this static transform so that depth_image_proc/register can get the TF easier
        transformStamped.header.stamp = time.add(Duration.fromMillis(100))
        return transformStamped
    }

    private fun algoTf2TfStamped(tfData: AlgoTfData, time: Time): TransformStamped {
        val vector3: Vector3 =
            mBridgeNode!!.mMessageFactory!!.newFromType(Vector3._TYPE)
        vector3.x = tfData.t.x.toDouble()
        vector3.y = tfData.t.y.toDouble()
        vector3.z = tfData.t.z.toDouble()
        val quaternion: Quaternion =
            mBridgeNode!!.mMessageFactory!!.newFromType(Quaternion._TYPE)
        quaternion.x = tfData.q.x.toDouble()
        quaternion.y = tfData.q.y.toDouble()
        quaternion.z = tfData.q.z.toDouble()
        quaternion.w = tfData.q.w.toDouble()
        val transform: Transform =
            mBridgeNode!!.mMessageFactory!!.newFromType(Transform._TYPE)
        transform.translation = vector3
        transform.rotation = quaternion
        val transformStamped: TransformStamped = mBridgeNode!!.mMessageFactory!!.newFromType(
            TransformStamped._TYPE
        )
        transformStamped.transform = transform
        transformStamped.childFrameId = tfData.tgtFrameID
        transformStamped.header.frameId = tfData.srcFrameID
        transformStamped.header.stamp = time
        return transformStamped
    }

    private fun produceOdometryMessage(
        tfData: AlgoTfData,
        linearVelocity: LinearVelocity,
        angularVelocity: AngularVelocity,
        time: Time
    ): Odometry { // Start assembling a nav_msg/Odometry
        val odom_message = mBridgeNode!!.mOdometryPubr!!.newMessage()
        odom_message.pose.pose.position.x = tfData.t.x.toDouble()
        odom_message.pose.pose.position.y = tfData.t.y.toDouble()
        odom_message.pose.pose.position.z = tfData.t.z.toDouble()
        odom_message.pose.pose.orientation.x = tfData.q.x.toDouble()
        odom_message.pose.pose.orientation.y = tfData.q.y.toDouble()
        odom_message.pose.pose.orientation.z = tfData.q.z.toDouble()
        odom_message.pose.pose.orientation.w = tfData.q.w.toDouble()
        // TODO: make sure it's fine to leave Y and Z uninitialized
// Segway can only travel in the X direction
        odom_message.twist.twist.linear.x = linearVelocity.speed.toDouble()
        // TODO: make sure it's fine to leave X and Y uninitialized
// Segway can only rotate around the Z axis
        odom_message.twist.twist.angular.z = angularVelocity.speed.toDouble()
        // Child frame is the frame of the twist: base link
// Add tf_prefix to each transform before ROS publishing (in case of multiple loomos on one network)
        if (mBridgeNode!!.use_tf_prefix) {
            odom_message.childFrameId = mBridgeNode!!.tf_prefix + "_" + "base_link"
        } else {
            odom_message.childFrameId = "base_link"
        }
        // parent frame is the odometry frame
// Add tf_prefix to each transform before ROS publishing (in case of multiple loomos on one network)
        if (mBridgeNode!!.use_tf_prefix) {
            odom_message.header.frameId = mBridgeNode!!.tf_prefix + "_" + "odom"
        } else {
            odom_message.header.frameId = "odom"
        }
        odom_message.header.stamp = time
        return odom_message
    }
}