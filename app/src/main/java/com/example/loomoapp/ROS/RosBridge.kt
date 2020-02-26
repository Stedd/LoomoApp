package com.example.loomoapp.ROS

interface RosBridge {
    fun node_started(mBridgeNode: RosBridgeNode)
    fun start()
    fun stop()
}