package com.example.loomoapp.ROS

object Utils {
    private const val TAG = "Utils"
    fun platformStampToSecond(stamp: Long): Double {
        return stamp.toDouble() / 1.0E6
    }

    fun platformStampInMillis(stamp: Long): Long {
        return (stamp.toDouble() / 1.0E3).toLong()
    }

    fun platformStampInNano(stamp: Long): Long {
        return stamp * 1000
    }
}