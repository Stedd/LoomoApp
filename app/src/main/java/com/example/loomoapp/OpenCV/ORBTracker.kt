package com.example.loomoapp.OpenCV

import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.Scalar
import org.opencv.features2d.Features2d
import org.opencv.features2d.ORB

class ORBTracker {
    private val TAG = "OpenCV/ORBTracker"

    private val detector: ORB = ORB.create()
    val keyPoints = MatOfKeyPoint()
    private val descriptors = Mat()


    fun onNewFrame(img: Mat): MatOfKeyPoint {
        detector.detect(img, keyPoints)
        detector.compute(img,keyPoints, descriptors)
//        Features2d.drawKeypoints(img, keyPoints, img, Scalar(0.0, 255.0, 0.0))
        return keyPoints
    }

}