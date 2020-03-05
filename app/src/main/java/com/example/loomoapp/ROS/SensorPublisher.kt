package com.example.loomoapp.ROS

import android.util.Log
import android.util.Pair
import com.example.loomoapp.Loomo.LoomoSensor
import com.example.loomoapp.Loomo.LoomoSensor.Companion.INFRARED_FOV
import com.example.loomoapp.Loomo.LoomoSensor.Companion.ULTRASONIC_FOV
import com.example.loomoapp.Loomo.LoomoSensor.Companion.ULTRASONIC_MAX_RANGE
import com.example.loomoapp.Loomo.LoomoSensor.Companion.ULTRASONIC_MIN_RANGE
import com.example.loomoapp.LoopedThread
import com.segway.robot.sdk.perception.sensor.Sensor
import com.segway.robot.sdk.vision.frame.FrameInfo
import org.ros.message.Duration
import org.ros.message.Time
import sensor_msgs.Range

/**
 * Created by mfe on 8/3/18.
 */
class SensorPublisher(sensor_: LoomoSensor, private val handlerThread: LoopedThread) : RosBridge {

    companion object {
        private const val TAG = "SensorPublisher"
    }

    var mIsStarted = false
    val sensor = sensor_.mSensor

    private var mBridgeNode: RosBridgeNode? = null
//    private var mSensorPublishThread = SensorPublisherThread()

    override fun node_started(mBridgeNode: RosBridgeNode) {
        this.mBridgeNode = mBridgeNode
        Log.d(TAG,"SensorPublisher node_started")
        start()
//        handlerThread.handler.postDelayed({publishSensors()}, 10000 )
    }

    override fun start() {
        mIsStarted = true
        Log.d(TAG, "start_sensor() $mIsStarted")
    }

    override fun stop() {
        Log.d(TAG, "stop_sensor()")
        mIsStarted = false
    }

    //Call functions to to publish ROS messages
    fun publishSensors(){
        publishUltrasonic()
        publishInfrared()
        publishBase()
    }

    fun publishUltrasonic(){
        Log.d(TAG,"Posting Ultrasonic message")
        handlerThread.handler.post(PublishUltrasonic(getTimeDiff()))
    }
    fun publishInfrared(){
        handlerThread.handler.post(PublishInfrared(getTimeDiff()))
    }
    fun publishBase(){
        handlerThread.handler.post(PublishBase())
    }

    private fun getTimeDiff():Duration{
        val currentRosTime          = mBridgeNode!!.mConnectedNode!!.currentTime
        val currentSystemTime       = Time.fromMillis(System.currentTimeMillis())
        return currentRosTime.subtract(currentSystemTime)
    }

    //Runnables
    inner class PublishUltrasonic(val timeDiff:Duration): Runnable {
        override fun run() {
            val mUltrasonicData = sensor.querySensorData(listOf(Sensor.ULTRASONIC_BODY))[0]
            var mUltrasonicDistance = mUltrasonicData.intData[0].toFloat()
            val ultrasonicMessage = mBridgeNode!!.mUltrasonicPubr!!.newMessage()
            val stampTime = Time.fromNano(Utils.platformStampInNano(mUltrasonicData.timestamp))
            val correctedStampTime = stampTime.add(timeDiff)
            ultrasonicMessage.header.frameId = mBridgeNode!!.UltrasonicFrame
            ultrasonicMessage.header.stamp = correctedStampTime
            // Ultrasonic sensor FOV is 40 degrees
            ultrasonicMessage.fieldOfView = (ULTRASONIC_FOV * (Math.PI / 180.0f)).toFloat()
            ultrasonicMessage.radiationType = Range.ULTRASOUND
            ultrasonicMessage.minRange = ULTRASONIC_MIN_RANGE // Min range is 250mm
            ultrasonicMessage.maxRange = ULTRASONIC_MAX_RANGE // Max range is 1500m
            // Clamp ultrasonic data to max range (ROS requires this)
            if (mUltrasonicDistance > 1500) {
                mUltrasonicDistance = 1500f
            }
            ultrasonicMessage.range =
                mUltrasonicDistance / 1000.0f // Loomo API provides data in mm
            mBridgeNode!!.mUltrasonicPubr!!.publish(ultrasonicMessage)
        }
    }

    inner class PublishInfrared(val timeDiff:Duration): Runnable {
        override fun run() {
            val infraredData = sensor.infraredDistance
            val mInfraredDistanceLeft = infraredData.leftDistance
            val mInfraredDistanceRight = infraredData.rightDistance
            val infraredMessageLeft = mBridgeNode!!.mInfraredPubrLeft!!.newMessage()
            val infraredMessageRight = mBridgeNode!!.mInfraredPubrRight!!.newMessage()
            val stampTime = Time.fromNano(Utils.platformStampInNano(infraredData.timestamp))
            val correctedStampTime = stampTime.add(timeDiff)
            infraredMessageLeft.header.stamp = correctedStampTime
            infraredMessageRight.header.stamp = correctedStampTime
            infraredMessageLeft.header.frameId = mBridgeNode!!.LeftInfraredFrame
            infraredMessageRight.header.frameId = mBridgeNode!!.RightInfraredFrame
            // TODO: get real FOV of infrared sensors
            infraredMessageLeft.fieldOfView = (INFRARED_FOV * (Math.PI / 180.0f)).toFloat()
            infraredMessageRight.fieldOfView = (INFRARED_FOV * (Math.PI / 180.0f)).toFloat()
            infraredMessageLeft.radiationType = Range.INFRARED
            infraredMessageRight.radiationType = Range.INFRARED
            infraredMessageLeft.range = mInfraredDistanceLeft / 1000.0f
            infraredMessageRight.range = mInfraredDistanceRight / 1000.0f
            mBridgeNode!!.mInfraredPubrLeft!!.publish(infraredMessageLeft)
            mBridgeNode!!.mInfraredPubrRight!!.publish(infraredMessageRight)
        }
    }

    inner class PublishBase(): Runnable {
        override fun run() {
            val mBaseImu = sensor.querySensorData(listOf(Sensor.BASE_IMU))[0]
            val mBasePitch = mBaseImu.floatData[0]
            val basePitchMessage = mBridgeNode!!.mBasePitchPubr!!.newMessage()
            basePitchMessage.data = mBasePitch
            mBridgeNode!!.mBasePitchPubr!!.publish(basePitchMessage)
        }
    }

//    private inner class SensorPublisherThread : Thread() {
//        override fun run() {
//            Log.d(TAG, "run: SensorPublisherThread")
////            super.run()
//            while (mIsStarted) { // No metadata for this frame yet
////                Log.d(TAG, "run: SensorPublisherThread")
//                // Get an appropriate ROS time to match the platform time of this stamp
//                val currentRosTime          = mBridgeNode!!.mConnectedNode!!.currentTime
//                val currentSystemTime       = Time.fromMillis(System.currentTimeMillis())
//                val rosToSystemTimeOffset   = currentRosTime.subtract(currentSystemTime)
//                if (mBridgeNode!!.should_pub_ultrasonic) {
//                    val mUltrasonicData =
////                        sensor_.getSurroundings().UltraSonic
//                        sensor.querySensorData(listOf(Sensor.ULTRASONIC_BODY))[0]
//
//                    var mUltrasonicDistance = mUltrasonicData.intData[0].toFloat()
//                    val ultrasonicMessage = mBridgeNode!!.mUltrasonicPubr!!.newMessage()
//                    val stampTime =
//                        Time.fromNano(Utils.platformStampInNano(mUltrasonicData.timestamp))
//                    val correctedStampTime =
//                        stampTime.add(rosToSystemTimeOffset)
//                    ultrasonicMessage.header.frameId = mBridgeNode!!.UltrasonicFrame
//                    ultrasonicMessage.header.stamp = correctedStampTime
//                    // Ultrasonic sensor FOV is 40 degrees
//                    ultrasonicMessage.fieldOfView = (40.0f * (Math.PI / 180.0f)).toFloat()
//                    ultrasonicMessage.radiationType = Range.ULTRASOUND
//                    ultrasonicMessage.minRange = 0.250f // Min range is 250mm
//                    ultrasonicMessage.maxRange = 1.5f // Max range is 1500m
//                    // Clamp ultrasonic data to max range (ROS requires this)
//                    if (mUltrasonicDistance > 1500) {
//                        mUltrasonicDistance = 1500f
//                    }
//                    ultrasonicMessage.range =
//                        mUltrasonicDistance / 1000.0f // Loomo API provides data in mm
//                    mBridgeNode!!.mUltrasonicPubr!!.publish(ultrasonicMessage)
//                }
//                if (mBridgeNode!!.should_pub_infrared) {
//                    val infraredData = sensor.infraredDistance
//                    val mInfraredDistanceLeft = infraredData.leftDistance
//                    val mInfraredDistanceRight = infraredData.rightDistance
//                    val infraredMessageLeft =
//                        mBridgeNode!!.mInfraredPubrLeft!!.newMessage()
//                    val infraredMessageRight =
//                        mBridgeNode!!.mInfraredPubrRight!!.newMessage()
//                    val stampTime =
//                        Time.fromNano(Utils.platformStampInNano(infraredData.timestamp))
//                    val correctedStampTime =
//                        stampTime.add(rosToSystemTimeOffset)
//                    infraredMessageLeft.header.stamp = correctedStampTime
//                    infraredMessageRight.header.stamp = correctedStampTime
//                    infraredMessageLeft.header.frameId = mBridgeNode!!.LeftInfraredFrame
//                    infraredMessageRight.header.frameId = mBridgeNode!!.RightInfraredFrame
//                    // TODO: get real FOV of infrared sensors
//                    infraredMessageLeft.fieldOfView = (40.0f * (Math.PI / 180.0f)).toFloat()
//                    infraredMessageRight.fieldOfView = (40.0f * (Math.PI / 180.0f)).toFloat()
//                    infraredMessageLeft.radiationType = Range.INFRARED
//                    infraredMessageRight.radiationType = Range.INFRARED
//                    infraredMessageLeft.range = mInfraredDistanceLeft / 1000.0f
//                    infraredMessageRight.range = mInfraredDistanceRight / 1000.0f
//                    mBridgeNode!!.mInfraredPubrLeft!!.publish(infraredMessageLeft)
//                    mBridgeNode!!.mInfraredPubrRight!!.publish(infraredMessageRight)
//                }
//                if (mBridgeNode!!.should_pub_base_pitch) {
//                    val mBaseImu =
//                        sensor.querySensorData(listOf(Sensor.BASE_IMU))[0]
//                    val mBasePitch = mBaseImu.floatData[0]
//                    //                    float mBaseRoll = mBaseImu.getFloatData()[1];
////                    float mBaseYaw = mBaseImu.getFloatData()[2];
//                    val basePitchMessage = mBridgeNode!!.mBasePitchPubr!!.newMessage()
//                    basePitchMessage.data = mBasePitch
//                    mBridgeNode!!.mBasePitchPubr!!.publish(basePitchMessage)
//                }
//                // TODO: 26/02/2020 test this
//                sleep(2000)
//            }
//        }
//    }


}