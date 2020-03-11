package com.example.loomoapp.ROS

import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.utils.copy
import com.example.loomoapp.utils.toByteArray
import com.segway.robot.sdk.vision.frame.FrameInfo
import java.nio.ByteBuffer

class RosMainPublisher(
    private val fishEyeByteBuffer: MutableLiveData<ByteBuffer>,
    private val colorByteBuffer: MutableLiveData<ByteBuffer>,
    private val depthByteBuffer: MutableLiveData<ByteBuffer>,
    private val fishEyeFrameInfo: MutableLiveData<FrameInfo>,
    private val colorFrameInfo: MutableLiveData<FrameInfo>,
    private val depthFrameInfo: MutableLiveData<FrameInfo>,
    private val mRealSensePublisher:RealsensePublisher,
    private val mSensorPublisher:SensorPublisher,
    private val mTFPublisher:TFPublisher){

    fun publishGraph(){
        // TODO: 05/03/2020 Add all the data we need for the SLAM Graphs
        val frameInfo = depthFrameInfo.value!! //Doesn't matter what frame is used
        mSensorPublisher.publishBase()
        mSensorPublisher.publishUltrasonic()
        mSensorPublisher.publishInfrared()
        mSensorPublisher.publishBase()
        mTFPublisher.publishOdometry(frameInfo)
        mTFPublisher.publishTF(frameInfo)
    }

    fun publishAllCameras(){
        publishFishEyeImage()
        publishColorImage()
        publishDepthImage()
    }

    fun publishFishEyeImage(){
        mRealSensePublisher.publishFishEyeImage(
            fishEyeByteBuffer.value!!.copy().toByteArray(),
            fishEyeFrameInfo.value!!
        )
    }
    fun publishColorImage(){
        mRealSensePublisher.publishColorImage(
            colorByteBuffer.value!!.copy().toByteArray(),
            colorFrameInfo.value!!
        )

    }
    fun publishDepthImage(){
        mRealSensePublisher.publishDepthImage(
            depthByteBuffer.value!!.copy().toByteArray(),
            depthFrameInfo.value!!
        )

    }
}
