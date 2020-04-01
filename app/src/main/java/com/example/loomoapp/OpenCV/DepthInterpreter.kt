package com.example.loomoapp.OpenCV

import org.opencv.core.Mat
import kotlin.math.floor

class DepthInterpreter(
    val width:Int,
    val height:Int,
    val numberOfRegions:Int
) {

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

    init {
        val widthOfRegion:Int = width/numberOfRegions
        val rest = width%numberOfRegions
    }


    fun processFrame(img: Mat){
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