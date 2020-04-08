package com.example.loomoapp.OpenCV

import android.util.Log
import org.opencv.calib3d.Calib3d.*
import org.opencv.core.*
import org.opencv.features2d.ORB
import org.opencv.video.Video

class ORBTracker {
    private val TAG = "OpenCV/ORBTracker"

    private val detector: ORB = ORB.create()
    private var keyPoints = MatOfKeyPoint()
    private val descriptors = Mat()
    private var points = MatOfPoint2f()
    private var prevPoints = MatOfPoint2f()
    private var pointsOG = MatOfPoint2f()
    private var status = MatOfByte()
    var totalReceivedFrames = 0
        private set

    private val MIN_NUM_OF_FEATURES = 50
    var expectedNumOfFeatures = 0
        private set
    var minNumOfFeatures = 50
        private set
    var numOfFeatures = 0
        private set

    private val prevImg = Mat()

    private var pointPair = Pair<MatOfPoint2f, MatOfPoint2f>(pointsOG, points)


    private var essentialMatrix = Mat()
    private var mask = Mat()
    private var relRotMat = Mat()
    private var relTranslMat = Mat()

    //    fun onNewFrame(img: Mat): MatOfKeyPoint {
    fun onNewFrame(img: Mat): Pair<MatOfPoint2f, MatOfPoint2f> {
        totalReceivedFrames++

        if (numOfFeatures < minNumOfFeatures) {
            detect(img)
        }

        trackFeatures(img)
        img.copyTo(prevImg)


//        return keyPoints
        return pointPair
    }


    private val alpha = 0.2
    private fun detect(img: Mat) {
        detector.detect(img, keyPoints)
        detector.compute(img, keyPoints, descriptors)
//        keyPoints.convertTo(points, CV_32F)
        val kpTmp = keyPoints.toArray()
        val p2fTmp = Array<Point>(kpTmp.size) { kpTmp[it].pt }
        points = MatOfPoint2f(*p2fTmp)
        points.copyTo(pointsOG)
        points.copyTo(prevPoints)
        numOfFeatures = kpTmp.size
        expectedNumOfFeatures =
            if (numOfFeatures > expectedNumOfFeatures) numOfFeatures
            else ((1 - alpha) * expectedNumOfFeatures + alpha * numOfFeatures).toInt()
        minNumOfFeatures = if ((0.5 * expectedNumOfFeatures).toInt() > MIN_NUM_OF_FEATURES) {
            (0.5 * expectedNumOfFeatures).toInt()
        } else {
            MIN_NUM_OF_FEATURES
        }
    }

    private fun trackFeatures(img: Mat) {
        val err = MatOfFloat()
        val winSize = Size(21.0, 21.0)
        val termCrit = TermCriteria(TermCriteria.COUNT or TermCriteria.EPS, 30, 0.1)

        if ((prevImg.empty()) or (prevPoints.size().area() <= 0)) {
            Log.d(
                TAG,
                "prevImg empty: ${prevImg.empty()}, or prevPoints size: ${prevPoints.size().area()}"
            )
            img.copyTo(prevImg)
            points.copyTo(prevPoints)
            return
        }


        try {
            Video.calcOpticalFlowPyrLK(
                prevImg,
                img,
                prevPoints,
                points,
                status,
                err,
                winSize,
                3,
                termCrit,
                0,
                0.001
            )
        } catch (e: Exception) {
            Log.d(TAG, "Something wrong with Video.calcOpticalFlowPyrLK")
            Thread.sleep(500)
            points.copyTo(prevPoints)
        }

        // Remove points where tracking failed, or where they have gone outside the frame
        val statusList = status.toList()
        val prevPointsList = prevPoints.toList()
        val pointsList = points.toList()


//        val tmpStatus = mutableListOf<Byte>()
        val tmpPointsOld = mutableListOf<Point>()
        val tmpPoints = mutableListOf<Point>()
        var indexCorrection = 0
        val pointsOGList = pointsOG.toList()
        val tmpOGlist = mutableListOf<Point>()
        for ((index, stat) in statusList.withIndex()) {
            val pt = prevPointsList[index - indexCorrection]
            if ((stat.toInt() == 0) or (pt.x < 0) or (pt.y < 0)) {
                if ((pt.x < 0) or (pt.y < 0)) {
                    statusList[index] = 0.toByte()
                }
                indexCorrection++
            } else {
//                tmpStatus.add(stat)
                tmpPointsOld.add(prevPointsList[index])
                tmpPoints.add(pointsList[index])
                tmpOGlist.add(pointsOGList[index])
            }
        }
        numOfFeatures = tmpPoints.size
        status = MatOfByte(*statusList.toByteArray())
        points = MatOfPoint2f(*tmpPoints.toTypedArray())
        prevPoints = MatOfPoint2f(*tmpPointsOld.toTypedArray())
        pointsOG = MatOfPoint2f(*tmpOGlist.toTypedArray())
        pointPair = Pair(pointsOG, points)

        points.copyTo(prevPoints)
    }

}
