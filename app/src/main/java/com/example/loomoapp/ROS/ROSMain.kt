package com.example.loomoapp.ROS

import android.util.Log
import android.util.Pair
import com.example.loomoapp.*
import org.ros.address.InetAddressFactory
import org.ros.android.RosActivity
import org.ros.message.Time
import org.ros.node.NodeConfiguration
import org.ros.node.NodeMainExecutor
import org.ros.time.NtpTimeProvider
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit

class ROSMain: RosActivity("LoomoROS", "LoomoROS", URI.create("http://192.168.2.31:11311/")) {


    lateinit var mRealsensePublisher: RealsensePublisher
    lateinit var mTFPublisher: TFPublisher
    lateinit var mSensorPublisher: SensorPublisher
    lateinit var mRosBridgeConsumers: List<RosBridge>
    lateinit var mBridgeNode: RosBridgeNode
    lateinit var mDepthRosStamps // Stores a co-ordinated platform time and ROS time to help manage the offset
            : Queue<Pair<Long, Time>>
    lateinit var mDepthStamps: Queue<Long>


    fun initMain(){
        //ROS
        // Keep track of timestamps when images published, so corresponding TFs can be published too
        mDepthStamps = ConcurrentLinkedDeque<Long>()
        mDepthRosStamps = ConcurrentLinkedDeque<Pair<Long, Time>>()
        mRealsensePublisher = RealsensePublisher(mDepthStamps, mDepthRosStamps)
        mTFPublisher = TFPublisher(mDepthStamps, mDepthRosStamps)
        mSensorPublisher = SensorPublisher()

        mRosBridgeConsumers = listOf(
            mRealsensePublisher,
            mTFPublisher,
            mSensorPublisher
        )
        val mOnNodeStarted = Runnable {
            Log.d(TAG, "node for loop")
            // Node has started, so we can now tell publishers and subscribers that ROS has initialized
            for (consumer in mRosBridgeConsumers) {
                consumer.node_started(mBridgeNode)
                // Try a call to start listening, this may fail if the Loomo SDK is not started yet (which is fine)
                consumer.start()
            }
        }

        // TODO: shutdown consumers correctly
        val mOnNodeShutdown = Runnable { }

        // Start an instance of the RosBridgeNode
        mBridgeNode = RosBridgeNode(mOnNodeStarted, mOnNodeShutdown)
    }

    override fun init(nodeMainExecutor: NodeMainExecutor) {
        Log.d(TAG, "Init ROS node.")
        val nodeConfiguration = NodeConfiguration.newPublic(
            InetAddressFactory.newNonLoopback().hostAddress,
            masterUri
        )
        // Note: NTPd on Linux will, by default, not allow NTP queries from the local networks.
        // Add a rule like this to /etc/ntp.conf:
        //
        // restrict 192.168.86.0 mask 255.255.255.0 nomodify notrap nopeer
        //
        // Where the IP address is based on your subnet
        val ntpTimeProvider = NtpTimeProvider(
            InetAddressFactory.newFromHostString(masterUri.host),
            nodeMainExecutor.scheduledExecutorService
        )
        try {
            ntpTimeProvider.updateTime()
        } catch (e: Exception) {
            Log.d(TAG, "exception when updating time...")
        }
        Log.d(TAG, "master uri: " + masterUri.host)
        Log.d(TAG, "ros: " + ntpTimeProvider.currentTime.toString())
        Log.d(
            TAG,
            "sys: " + Time.fromMillis(System.currentTimeMillis())
        )
        ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES)
        nodeConfiguration.timeProvider = ntpTimeProvider
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration)
    }


    fun startConsumers(){
        for (consumer in mRosBridgeConsumers) {
            consumer.node_started(mBridgeNode)
            // Try a call to start listening, this may fail if the Loomo SDK is not started yet (which is fine)
            consumer.start()
        }
    }

}