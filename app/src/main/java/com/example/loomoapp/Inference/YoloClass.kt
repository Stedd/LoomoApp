package com.example.loomoapp.Inference

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.Loomo.LoomoRealSense
import org.opencv.dnn.Net

class YoloClass {

    //constants

    var startYolo = false
    var firstTimeYolo = false
    var tinyYolo: Net? = null

    //variables


        private lateinit var inferenceImageViewBitmap: MutableLiveData<Bitmap>
        private var inferenceImage: Bitmap = Bitmap.createBitmap(
            LoomoRealSense.FISHEYE_WIDTH,
            LoomoRealSense.FISHEYE_HEIGHT,Bitmap.Config.ARGB_8888)



        // alt det andre som stod i funksjonen


}