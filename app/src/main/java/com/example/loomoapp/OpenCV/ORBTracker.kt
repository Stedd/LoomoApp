package com.example.loomoapp.OpenCV

import android.util.Log
import org.opencv.core.*
import org.opencv.core.CvType.CV_32F
import org.opencv.features2d.ORB
import org.opencv.video.Video

class ORBTracker {
    private val TAG = "OpenCV/ORBTracker"

    private val detector: ORB = ORB.create()
    private val keyPoints = MatOfKeyPoint()
    private val descriptors = Mat()
    private val points = MatOfPoint2f()
    private val pointsOld = MatOfPoint2f()

    private val prevImg = Mat()

    fun onNewFrame(img: Mat): MatOfKeyPoint {
        detect(img)

        trackFeatures(img)

        return keyPoints
    }


    private fun detect(img: Mat) {
        detector.detect(img, keyPoints)
        detector.compute(img, keyPoints, descriptors)
        keyPoints.convertTo(points, CV_32F)
    }

    private fun trackFeatures(img: Mat) {
        val err = MatOfFloat()
        val winSize = Size(21.0, 21.0)
        val termCrit = TermCriteria(TermCriteria.COUNT or TermCriteria.EPS, 30, 0.1)

//        if (prevImg.empty()) {
//            img.copyTo(prevImg)
//            points.copyTo(pointsOld)
//            return
//        }

//        if (pointsOld.checkVector(2, CV_32F, true) < 0) {
//            //what da heck!
//            Log.d(TAG, "Something wrong with points")
//            Thread.sleep(500) // Just so the Logcat is not utterly spammed
//            return
//        }

        val status = MatOfByte()

//        val pyr = MutableList(3) {Mat()}
//        val lvls = Video.buildOpticalFlowPyramid(img, pyr, img.size(), 3)
//        Video.buildOpticalFlowPyramid(prevImg, pyr, img.size(), 3)

//        try {
//            Video.calcOpticalFlowPyrLK(
//                prevImg,
//                img,
//                points,
//                points,
//                status,
//                err,
//                winSize,
//                3,
//                termCrit,
//                0,
//                0.001
//            )
//        } catch (e: Exception) {
//            Log.d(TAG, "Something wrong with PointsOld")
//            Thread.sleep(500)
//        }

        // Remove points where tracking failed, or where they have gone outside the frame
//        var indexCorrection = 0
//        for (col in 0..status.size().width.toInt()) {
//            for (row in 0..status.size().height.toInt()) {
//                val pt = pointsOld.get(row, col)
//                if ((status.get(row,col)[0].toInt() == 0) or (pt[0] < 0) or (pt[1] < 0)) {
//                    if ((pt[0] < 0) or (pt[1] < 0)) {
//                        status[row, col][0] = 0.0
//                    }
//                    points.toList().removeAt(Point(row,col))
//                }
//            }
//        }

    }

}