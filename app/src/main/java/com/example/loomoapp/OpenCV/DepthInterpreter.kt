package com.example.loomoapp.OpenCV

import android.util.Log
import org.opencv.core.Mat

class DepthInterpreter(
    val width: Int,
    val height: Int,
    val numberOfRegions: Int
) {

    companion object {
        private const val TAG = "DepthInterpreter"
    }

    //Class for converting a depth camera frame to an array which represents the average depth of each sector of the image

    /*
    Tasks this class has to perform:
    1. Fetch the depth frame Mat
    2. Separate the Mat into regions, (Need to decide how many regions are needed)
    3. Process each region
        a. Remove dark spots
        b. get average value of the region
    4. Send result of all regions to the LoomoControl Class.
     */

    //Variables
    lateinit var depthImage: Mat
    lateinit var regions: Array<Mat>
    // TODO: 02.04.2020 opencv ROI

    init {
        val widthOfRegion: Int = width / numberOfRegions
        val rest = width % numberOfRegions
    }


    fun processFrame(img: Mat) {
        // TODO: 01.04.2020 Nested for loop which separates the Mat into Regions
        var sum = 0
        var sum0 = 0
        var sum1 = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
//                for (ch in 0..1 ){
//                    sum += (img.get(row, col)[0].toInt())
                Log.d(TAG, "${img.get(row, col)}");
//                    sum += (img.get(row, col))
//                    sum += ch
//                }
//
//                sum0 += (img.get(row, col)[0].toInt())
//                sum1 += (img.get(row, col)[0].toInt())
//                sum+=img.get(row, col)[1].toInt()
//                Log.d(TAG, "sum: ${img.get(row, col)[0].toInt()}")
            }
        }
//        Log.d(TAG, "sum0: $sum0 sum1: $sum1");
//        Log.d(TAG, "sum0: $sum");
        Log.d(TAG, "sum0: $sum");
    }
//
//    fun processFrame(img: Mat) {
//        var sum = 0
//        for (region in 0 until numberOfRegions) {
//            region * numberOfRegions
//            for (row in 0 until height) {
//                for (col in 0 until width) {
//
//                    Log.d(TAG, "row: $row, col:$col. val:$value");
//                    sum += img.get(row, col)[0].toInt()
//                }
//            }
//        }
//
//        Log.d(TAG, "sum: $sum");
//    }
}