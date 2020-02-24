package com.example.loomoapp.Loomo

/**
 * A class that is meant to make the Intel RealSense available for the Loomo
 */


/**
 * "If you need a singleton - a class that only has got one instance - you can declare the
 * class in the usual way, but use the object keyword instead of class"
 * https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html
 */
object LoomoRealSense {
    val TAG = "LoomoRealSense"

    const val COLOR_WIDTH = 640
    const val COLOR_HEIGHT = 480

    const val FISHEYE_WIDTH = 640
    const val FISHEYE_HEIGHT = 480

    const val SMALL_COLOR_WIDTH = 320
    const val SMALL_COLOR_HEIGHT = 240

    const val DEPTH_WIDTH = 320
    const val DEPTH_HEIGHT = 240

}