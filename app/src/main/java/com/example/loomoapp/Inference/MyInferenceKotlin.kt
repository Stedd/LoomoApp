package com.example.loomoapp.Inference

import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_WIDTH
import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.CvType.CV_8UC3
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR5652RGB
import org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX
import org.opencv.utils.Converters
import java.util.*

class MyInferenceKotlin {

    //constants
    private val TAG = "MyInferenceKotlin"


    //variables
    var inferenceImage = Mat.zeros(FISHEYE_HEIGHT, FISHEYE_WIDTH,  CV_8UC3)
        private set

   // var firstTimeYolo = false
    var startYolo = false
    var tinyYolo: Net = Net()
    private val result: List<Mat> = ArrayList(2)
    private val outBlobNames: MutableList<String> = ArrayList(2)

    private val confThreshold = 0.3f
    private val clsIds: MutableList<Int> = ArrayList()
    private val confs: MutableList<Float> = ArrayList()
    private val rects: MutableList<Rect> = ArrayList()


    fun onFisheyeCameraFrame(inputFrame: Mat) {

//        inferenceImage = inputFrame.clone()

        onCameraFrame(inputFrame)
    }





    private fun onCameraFrame(frame: Mat) {
        if (startYolo) {
            Imgproc.cvtColor(frame, inferenceImage, Imgproc.COLOR_GRAY2RGB)
            val imageBlob = Dnn.blobFromImage(
                inferenceImage,
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




            for (i in result.indices) {
                val level = result[i]
                for (j in 0 until level.rows()) {
                    val row = level.row(j)
                    val scores = row.colRange(5, level.cols())
                    val mm = Core.minMaxLoc(scores)
                    val confidence = mm.maxVal.toFloat()
                    val classIdPoint = mm.maxLoc
                    if (confidence > confThreshold) {
                        val centerX = (row[0, 0][0] * inferenceImage.cols()).toInt()
                        val centerY = (row[0, 1][0] * inferenceImage.rows()).toInt()
                        val width = (row[0, 2][0] * inferenceImage.cols()).toInt()
                        val height = (row[0, 3][0] * inferenceImage.rows()).toInt()
                        val left = centerX - width / 2
                        val top = centerY - height / 2
                        clsIds.add(classIdPoint.x.toInt())
                        confs.add(confidence)
                        rects.add(Rect(left, top, width, height))
                    }
                }
            }


//            val arrayLength = confs.size
            if (confs.size >= 1) {
                // Apply non-maximum suppression procedure.
                val nmsThresh = 0.2f
                val confidences = MatOfFloat(Converters.vector_float_to_Mat(confs))
                val boxesArray = rects.toTypedArray()
                val boxes = MatOfRect(*boxesArray)
                val indices = MatOfInt()
                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices)


                // Draw result boxes:
//                val ind = indices.toArray()
//                for (i in ind.indices) {
                for (i in indices.toArray()) {
                    val box = boxesArray[i]
                    val idGuy = clsIds[i]
                    val conf = confs[i]

//                    val idx = ind[i]
//                    val box = boxesArray[idx]
//                    val idGuy = clsIds[idx]
//                    val conf = confs[idx]
                    val cocoNames = listOf("a Loomo")
                    val intConf = (conf * 100).toInt()
                    Imgproc.putText(
                        inferenceImage,
                        cocoNames[idGuy].toString() + " " + intConf + "%",
                        box.tl(),
                        FONT_HERSHEY_SIMPLEX,
                        2.0,
                        Scalar(255.0, 255.0, 0.0),
                        2
                    )
                    Imgproc.rectangle(inferenceImage, box.tl(), box.br(), Scalar(255.0, 0.0, 0.0), 2)
                }
            }
        }
    }

}