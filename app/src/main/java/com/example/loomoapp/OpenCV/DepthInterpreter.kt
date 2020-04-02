package com.example.loomoapp.OpenCV

import android.util.Log
import org.opencv.core.Mat
import kotlin.math.floor
import kotlin.math.log

class DepthInterpreter(
    val img: Mat,
    val width:Int,
    val height:Int,
    val numberOfRegions:Int
) {

    companion object{
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
        val widthOfRegion:Int = width/numberOfRegions
        val rest = width%numberOfRegions
        var sum = 0
        for (region in 0 until numberOfRegions){
            region*numberOfRegions
            for (row in 0 until height){
                for (col in 0 until width){
                    val value = img.get(row,col)[0].toInt()
//                Log.d(TAG, "row: $row, col:$col. val:$value");
                    sum += value
                }
            }
        }

        Log.d(TAG, "sum: $sum");
    }


    fun processFrame(){
        Log.d(TAG, "image received");
        depthImage = img

        //Need a function for getting the value of each pixel

        //Separate into regions
        // TODO: 01.04.2020 Nested for loop which separates the Mat into Regions
        //skip for now




    }


    //Fetch the depth frame
    fun newFrame(img: Mat){
        depthImage = img
    }


}