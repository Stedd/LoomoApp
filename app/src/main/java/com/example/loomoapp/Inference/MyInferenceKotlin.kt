package com.example.loomoapp.Inference

import android.graphics.Bitmap
import androidx.core.graphics.set
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.Loomo.LoomoRealSense
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import java.util.*

class MyInferenceKotlin {

    //constants


    //variables
    private lateinit var inferenceImageViewBitmap: MutableLiveData<Bitmap>
    private var  inferenceImage = Mat()









    fun onFisheyeCameraFrame(inputFrame: Mat): Mat? {

        inferenceImage = inputFrame.clone()

        return inputFrame
    }




    fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        val frame = inputFrame.rgba()
        val startYolo = false
        val tinyYolo: Net? = null
        if (startYolo == true) {
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)
            val imageBlob = Dnn.blobFromImage(
                frame,
                0.00392,
                Size(416, 416),
                Scalar(0, 0, 0),  /*swapRB*/
                false,  /*crop*/
                false
            )

            tinyYolo!!.setInput(imageBlob)

            val result: List<Mat> = ArrayList(2)
            val outBlobNames: List<String> =
                ArrayList()
            outBlobNames.add(0, "yolo_16")
            outBlobNames.add(1, "yolo_23")
            tinyYolo.forward(result, outBlobNames)


            val confThreshold = 0.3f
            val clsIds: MutableList<Int> = ArrayList()
            val confs: MutableList<Float> =
                ArrayList()
            val rects: MutableList<Rect> =
                ArrayList()

            for (i in result.indices) {
                val level = result[i]
                for (j in 0 until level.rows()) {
                    val row = level.row(j)
                    val scores = row.colRange(5, level.cols())
                    val mm = Core.minMaxLoc(scores)
                    val confidence = mm.maxVal.toFloat()
                    val classIdPoint = mm.maxLoc
                    if (confidence > confThreshold) {
                        val centerX = (row[0, 0][0] * frame.cols()).toInt()
                        val centerY = (row[0, 1][0] * frame.rows()).toInt()
                        val width = (row[0, 2][0] * frame.cols()).toInt()
                        val height = (row[0, 3][0] * frame.rows()).toInt()
                        val left = centerX - width / 2
                        val top = centerY - height / 2
                        clsIds.add(classIdPoint.x.toInt())
                        confs.add(confidence)
                        rects.add(Rect(left, top, width, height))
                    }
                }
            }


            val ArrayLength = confs.size
            if (ArrayLength >= 1) {
                // Apply non-maximum suppression procedure.
                val nmsThresh = 0.2f
                val confidences = MatOfFloat(Converters.vector_float_to_Mat(confs))
                val boxesArray = rects.toTypedArray()
                val boxes = MatOfRect(*boxesArray)
                val indices = MatOfInt()
                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices)


                // Draw result boxes:
                val ind = indices.toArray()
                for (i in ind.indices) {
                    val idx = ind[i]
                    val box = boxesArray[idx]
                    val idGuy = clsIds[idx]
                    val conf = confs[idx]
                    val cocoNames =
                        Arrays.asList(
                            "a person",
                            "a bicycle",
                            "a motorbike",
                            "an airplane",
                            "a bus",
                            "a train",
                            "a truck",
                            "a boat",
                            "a traffic light",
                            "a fire hydrant",
                            "a stop sign",
                            "a parking meter",
                            "a car",
                            "a bench",
                            "a bird",
                            "a cat",
                            "a dog",
                            "a horse",
                            "a sheep",
                            "a cow",
                            "an elephant",
                            "a bear",
                            "a zebra",
                            "a giraffe",
                            "a backpack",
                            "an umbrella",
                            "a handbag",
                            "a tie",
                            "a suitcase",
                            "a frisbee",
                            "skis",
                            "a snowboard",
                            "a sports ball",
                            "a kite",
                            "a baseball bat",
                            "a baseball glove",
                            "a skateboard",
                            "a surfboard",
                            "a tennis racket",
                            "a bottle",
                            "a wine glass",
                            "a cup",
                            "a fork",
                            "a knife",
                            "a spoon",
                            "a bowl",
                            "a banana",
                            "an apple",
                            "a sandwich",
                            "an orange",
                            "broccoli",
                            "a carrot",
                            "a hot dog",
                            "a pizza",
                            "a doughnut",
                            "a cake",
                            "a chair",
                            "a sofa",
                            "a potted plant",
                            "a bed",
                            "a dining table",
                            "a toilet",
                            "a TV monitor",
                            "a laptop",
                            "a computer mouse",
                            "a remote control",
                            "a keyboard",
                            "a cell phone",
                            "a microwave",
                            "an oven",
                            "a toaster",
                            "a sink",
                            "a refrigerator",
                            "a book",
                            "a clock",
                            "a vase",
                            "a pair of scissors",
                            "a teddy bear",
                            "a hair drier",
                            "a toothbrush"
                        )
                    val intConf = (conf * 100).toInt()
                    Imgproc.putText(
                        frame,
                        cocoNames[idGuy].toString() + " " + intConf + "%",
                        box.tl(),
                        Core.FONT_HERSHEY_SIMPLEX,
                        2.0,
                        Scalar(255, 255, 0),
                        2
                    )
                    Imgproc.rectangle(frame, box.tl(), box.br(), Scalar(255, 0, 0), 2)
                }
            }
        }
        return frame
    }

}