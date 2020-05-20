package com.example.loomoapp.Inference

import android.util.Log
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_WIDTH
import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.CvType.CV_8UC3
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR5652RGB
import org.opencv.core.Core.FONT_HERSHEY_SIMPLEX
import org.opencv.core.CvType.CV_8UC3
import org.opencv.imgproc.Imgproc.circle
import org.opencv.utils.Converters
import java.util.*

class MyInferenceKotlin {

    //constants
    private val TAG = "MyInferenceKotlin"


    //variables
    var inferenceImage = Mat.zeros(FISHEYE_HEIGHT, FISHEYE_WIDTH, CV_8UC3)
        private set

    // var firstTimeYolo = false
    var startYolo = true
    var tinyYolo: Net = Net()
    private val result: List<Mat> = ArrayList(2)
    private val outBlobNames = mutableListOf<String>("", "")
    // private val outBlobNames: MutableList<String> = ArrayList(2)

    private val confThreshold = 0.1f
    private val clsIds: MutableList<Int> = ArrayList()
    private val confs: MutableList<Float> = ArrayList()
    private val rects: MutableList<Rect> = ArrayList()


    fun onFisheyeCameraFrame(inputFrame: Mat) {

//        inferenceImage = inputFrame.clone()

        onCameraFrame(inputFrame)
    }


    private fun onCameraFrame(frame: Mat) {
        if (startYolo) {

            val tmpimage = Mat()
              Imgproc.cvtColor(frame, tmpimage, Imgproc.COLOR_GRAY2RGB)
           // circle(inferenceImage, Point(100.0, 100.0), 10, Scalar(55.0, 0.0, 255.0), 3)

            val imageBlob = Dnn.blobFromImage(
                tmpimage,
                0.00392,
                Size(416.0, 416.0),
                Scalar(0.0, 0.0, 0.0),  /*swapRB*/
                false,  /*crop*/
                false
            )

            clsIds.clear()
            confs.clear()
            rects.clear()

            tinyYolo.setInput(imageBlob)


//            outBlobNames.add(0, "yolo_16")
//            outBlobNames.add(1, "yolo_23")
            outBlobNames[0] = "yolo_16"
            outBlobNames[1] = "yolo_23"
            tinyYolo.forward(result, outBlobNames)

           // Log.d(TAG, "tinyyolo: ${tinyYolo} , nativeObjAddr: ${tinyYolo.nativeObjAddr} ,unnconnectedoutlayers: ${tinyYolo.unconnectedOutLayers} , unnconnectedOutputlayersNames ${tinyYolo.unconnectedOutLayersNames}")



            for (i in result.indices) {
                //   Log.d(TAG, "hey ${result.indices}")
                val level = result[i]
                //   Log.d(TAG, "level: ${level}")

                for (j in 0 until level.rows()) {
                    val row = level.row(j)
                    val scores = row.colRange(5, level.cols())
                    val mm = Core.minMaxLoc(scores)
                    val confidence = mm.maxVal.toFloat()
                    val classIdPoint = mm.maxLoc
                    if (confidence > confThreshold) {
                        val centerX = (row[0, 0][0] * tmpimage.cols()).toInt()
                        val centerY = (row[0, 1][0] * tmpimage.rows()).toInt()
                        val width = (row[0, 2][0] * tmpimage.cols()).toInt()
                        val height = (row[0, 3][0] * tmpimage.rows()).toInt()
                        val left = centerX - width / 2
                        val top = centerY - height / 2
                        clsIds.add(classIdPoint.x.toInt())
                        confs.add(confidence)
                        rects.add(Rect(left, top, width, height))
                          //Log.d(TAG, "classID: ${clsIds} and confs: ${confs} and rects: ${rects}) conf.size is: ${confs.size}")

                        // Log.d(TAG, "row: ${row.dump()} and score: ${scores} and maxscore: ${mm} and " +
                         //        "confidence: ${confidence} and classIDpoint: ${classIdPoint}" )
                    }
                }
            }


//            val arrayLength = confs.size
            if (confs.size >= 1) {
                // Apply non-maximum suppression procedure.
                val nmsThresh = 0.1f
                val confidences = MatOfFloat(Converters.vector_float_to_Mat(confs))
                val boxesArray = rects.toTypedArray()
                val boxes = MatOfRect(*boxesArray)
                val indices = MatOfInt()
                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices)
                //   Log.d(TAG, "nmsThresh: ${nmsThresh} and confidences: ${confidences} and boxesarray ${boxesArray} and boxes ${boxes} and indices: ${indices}")


                // Draw result boxes:
//                val ind = indices.toArray()
//                for (i in ind.indices) {
                for (i in indices.toArray()) {
                    val box = boxesArray[i]
                    val idGuy = clsIds[i]
                    val conf = confs[i]
                  //  Log.d(TAG, "box: ${box} and idGuy ${idGuy} and conf: ${conf}")

//                    val idx = ind[i]
//                    val box = boxesArray[idx]
//                    val idGuy = clsIds[idx]
//                    val conf = confs[idx]
                    val cocoNames = listOf(
                        "Loomo"
                    )
                    val intConf = (conf * 100).toInt()
                    Imgproc.putText(
                        tmpimage,
                        cocoNames[idGuy].toString() + " " + intConf + "%",
                        box.tl(),
                        FONT_HERSHEY_SIMPLEX,
                        0.8,
                        Scalar(0.0, 200.0, 0.0),
                        2
                    )
                    Imgproc.rectangle(
                        tmpimage,
                        box.tl(),
                        box.br(),
                        Scalar(255.0, 255.0, 255.0),
                        3
                    )


                }

            }

            inferenceImage = tmpimage.clone()

        }
    }
}