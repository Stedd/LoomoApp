package com.example.loomoapp.ROS

public interface RosBridge {
    fun node_started(mBridgeNode: LoomoRosBridgeNode?)
    fun start()
    fun stop()
}