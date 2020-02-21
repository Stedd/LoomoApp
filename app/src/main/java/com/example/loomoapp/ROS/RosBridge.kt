package com.example.loomoapp.ROS

public interface RosBridge {
    fun node_started(mBridgeNode: RosBridgeNode)
    fun start()
    fun stop()
}