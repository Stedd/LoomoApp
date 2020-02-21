package com.example.loomoapp.ROS

import android.util.Log
import com.segway.robot.sdk.perception.sensor.Sensor
import org.ros.message.Time
import sensor_msgs.Range
import java.util.*

/**
 * Created by mfe on 8/3/18.
 */
class SensorPublisher : RosBridge {
    var mIsStarted = false
    private var mSensor: Sensor? = null
    private var mBridgeNode: LoomoRosBridgeNode? = null
    private var mSensorPublishThread: Thread? = null
    fun loomo_started(mSensor: Sensor?) {
        this.mSensor = mSensor
    }

    override fun node_started(mBridgeNode: LoomoRosBridgeNode) {
        this.mBridgeNode = mBridgeNode
    }

    override fun start() {
        if (mSensor == null || mBridgeNode == null || mIsStarted) {
            Log.d(
                TAG,
                "Cannot start_listening yet, a required service is not ready"
            )
            return
        }
        mIsStarted = true
        Log.d(TAG, "start_sensor()")
        if (mSensorPublishThread == null) {
            mSensorPublishThread = SensorPublisherThread()
        }
        mSensorPublishThread!!.start()
    }

    override fun stop() {
        if (!mIsStarted) {
            Log.d(TAG, "Cannot stop without starting")
            return
        }
        Log.d(TAG, "stop_sensor()")
        try {
            mSensorPublishThread!!.join()
        } catch (e: InterruptedException) {
            Log.w(
                TAG,
                "onUnbind: mSensorPublishThread.join() ",
                e
            )
        }
        mIsStarted = false
    }

    private inner class SensorPublisherThread : Thread() {
        override fun run() {
            Log.d(TAG, "run: SensorPublisherThread")
            super.run()
            while (null != mSensor) { // No metadata for this frame yet
// Get an appropriate ROS time to match the platform time of this stamp
                val currentRosTime =
                    mBridgeNode!!.mConnectedNode!!.currentTime
                val currentSystemTime =
                    Time.fromMillis(System.currentTimeMillis())
                val rosToSystemTimeOffset =
                    currentRosTime.subtract(currentSystemTime)
                if (mBridgeNode!!.should_pub_ultrasonic) { //                    TODO: Her hentes ultralyd sensor data gjennom Loomo API
                    val mUltrasonicData =
                        mSensor!!.querySensorData(Arrays.asList(Sensor.ULTRASONIC_BODY))[0]
                    var mUltrasonicDistance = mUltrasonicData.intData[0].toFloat()
                    val ultrasonicMessage =
                        mBridgeNode!!.mUltrasonicPubr!!.newMessage()
                    val stampTime =
                        Time.fromNano(Utils.platformStampInNano(mUltrasonicData.timestamp))
                    val correctedStampTime =
                        stampTime.add(rosToSystemTimeOffset)
                    ultrasonicMessage.header.frameId = mBridgeNode!!.UltrasonicFrame
                    ultrasonicMessage.header.stamp = correctedStampTime
                    // Ultrasonic sensor FOV is 40 degrees
                    ultrasonicMessage.fieldOfView = (40.0f * (Math.PI / 180.0f)).toFloat()
                    ultrasonicMessage.radiationType = Range.ULTRASOUND
                    ultrasonicMessage.minRange = 0.250f // Min range is 250mm
                    ultrasonicMessage.maxRange = 1.5f // Max range is 1500m
                    // Clamp ultrasonic data to max range (ROS requires this)
                    if (mUltrasonicDistance > 1500) {
                        mUltrasonicDistance = 1500f
                    }
                    ultrasonicMessage.range =
                        mUltrasonicDistance / 1000.0f // Loomo API provides data in mm
                    mBridgeNode!!.mUltrasonicPubr!!.publish(ultrasonicMessage)
                }
                if (mBridgeNode!!.should_pub_infrared) {
                    val infraredData = mSensor!!.infraredDistance
                    val mInfraredDistanceLeft = infraredData.leftDistance
                    val mInfraredDistanceRight = infraredData.rightDistance
                    val infraredMessageLeft =
                        mBridgeNode!!.mInfraredPubrLeft!!.newMessage()
                    val infraredMessageRight =
                        mBridgeNode!!.mInfraredPubrRight!!.newMessage()
                    val stampTime =
                        Time.fromNano(Utils.platformStampInNano(infraredData.timestamp))
                    val correctedStampTime =
                        stampTime.add(rosToSystemTimeOffset)
                    infraredMessageLeft.header.stamp = correctedStampTime
                    infraredMessageRight.header.stamp = correctedStampTime
                    infraredMessageLeft.header.frameId = mBridgeNode!!.LeftInfraredFrame
                    infraredMessageRight.header.frameId = mBridgeNode!!.RightInfraredFrame
                    // TODO: get real FOV of infrared sensors
                    infraredMessageLeft.fieldOfView = (40.0f * (Math.PI / 180.0f)).toFloat()
                    infraredMessageRight.fieldOfView = (40.0f * (Math.PI / 180.0f)).toFloat()
                    infraredMessageLeft.radiationType = Range.INFRARED
                    infraredMessageRight.radiationType = Range.INFRARED
                    infraredMessageLeft.range = mInfraredDistanceLeft / 1000.0f
                    infraredMessageRight.range = mInfraredDistanceRight / 1000.0f
                    mBridgeNode!!.mInfraredPubrLeft!!.publish(infraredMessageLeft)
                    mBridgeNode!!.mInfraredPubrRight!!.publish(infraredMessageRight)
                }
                if (mBridgeNode!!.should_pub_base_pitch) {
                    val mBaseImu =
                        mSensor!!.querySensorData(Arrays.asList(Sensor.BASE_IMU))[0]
                    val mBasePitch = mBaseImu.floatData[0]
                    //                    float mBaseRoll = mBaseImu.getFloatData()[1];
//                    float mBaseYaw = mBaseImu.getFloatData()[2];
                    val basePitchMessage = mBridgeNode!!.mBasePitchPubr!!.newMessage()
                    basePitchMessage.data = mBasePitch
                    mBridgeNode!!.mBasePitchPubr!!.publish(basePitchMessage)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SensorPublisher"
    }
}