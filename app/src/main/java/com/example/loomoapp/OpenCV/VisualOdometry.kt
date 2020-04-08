package com.example.loomoapp.OpenCV

import android.util.Log
import com.example.loomoapp.utils.RingBuffer
import org.opencv.calib3d.Calib3d.*
import org.opencv.core.*
import org.opencv.core.Core.gemm
import org.opencv.core.CvType.*
import org.opencv.imgproc.Imgproc

object VisualOdometry {
    private val TAG = "OpenCV/VisualOdometry"

    var drawOnImages = false

    val tracker = ORBTracker()

    private val posMat = Mat.zeros(3, 1, CV_64FC1)
    private val rotMat = Mat.eye(3, 3, CV_64FC1)

    private var essentialMatrix = Mat()
    private var mask = Mat.zeros(1, 10, CV_8U)
    private var relRotMat = Mat()
    private var relTranslMat = Mat()
    private var points1 = MatOfPoint2f()
    private var points2 = MatOfPoint2f()
    private var pointPair = Pair(points1, points2)

    var posHistory = RingBuffer<Mat>(100, true)
        private set
    var rotHistory = RingBuffer<Mat>(100, true)
        private set


    private var skipFrames = 0
    fun onNewFrame(img: Mat) {
        val pointPairTmp = tracker.onNewFrame(img)
        pointPair = Pair(pointPair.second, pointPairTmp.second)
        if (pointPair.first.type() != pointPair.second.type()) {
            Log.d(TAG, "pointPair types: ${pointPair.first.type()} != ${pointPair.second.type()}")
            return
        } else if (pointPair.first.size().area() < 0) {
            Log.d(TAG, "${pointPair.first.size().area()}")
            return
        }
        if (skipFrames < 10) {
            skipFrames++
            return
        }

//        if (pointPair.second.size() == pointPair.first.size()) {
        try {
//            Log.d(TAG, "Mask pre: [${mask[0, 0][0]}, ${mask[0, 1][0]}, ${mask[0, 2][0]}]")
//            Log.d(TAG, "Mask pre: ${mask.rows()} x ${mask.cols()}, ${typeToString(mask.type())}")
            essentialMatrix = findEssentialMat(
                pointPair.first,
                pointPair.second,
                fisheyeCameraMatrix,
                RANSAC,
                0.999,
                1.0,
                mask
            )
//            Log.d(TAG, "Mask post: ${mask.rows()} x ${mask.cols()}, ${typeToString(mask.type())}")
            recoverPose(
                essentialMatrix,
                pointPair.first,
                pointPair.second,
                fisheyeCameraMatrix,
                relRotMat,
                relTranslMat,
                mask
            )
            updatePose()
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
//        } else {
//            Log.d(TAG, "Did not perform Calib3d-stuff")
//        }
    }

    private fun updatePose() {
//        val pos = Mat(pose[pose.lastIndex], Rect(Point(1.0, 4.0), Point(3.0, 4.0)))
//        var rot = Mat(pose[pose.lastIndex], Rect(Point(1.0, 1.0), Point(3.0, 3.0)))
        Log.d(TAG, "Updating pose")
        val pos = if (posHistory.isEmpty()) {
            Log.d(TAG, "posHistory is empty")
            posMat
//            Mat.zeros(relTranslMat.size(), relTranslMat.type())
        } else {
            posHistory.peek()
        }
        val rot = if (rotHistory.isEmpty()) {
            Log.d(TAG, "rotHistory is empty")
            rotMat
//            Mat.zeros(relRotMat.size(), relRotMat.type())
        } else {
            rotHistory.peek()
        }
        Log.d(
            TAG, """
                |relTranslMat^T = ${relTranslMat.t().dump()}
                |relRotMat =
                |${relRotMat.dump()}
                |rot = 
                |${rot.dump()}
            """.trimMargin()
        )
        Log.d(TAG, "gemm 1")
        gemm(rot, relTranslMat, 1.0, pos, 1.0, pos)
        Log.d(TAG, "pos: ${pos.t().dump()}")
        Log.d(TAG, "gemm 2")
        gemm(relRotMat, rot, 1.0, Mat.zeros(3,3, CV_64FC1), 0.0, rot)


        posHistory.enqueue(pos)
        rotHistory.enqueue(rot)
    }

    fun drawTrajectory(): Mat {
        val trajectoryImg = Mat.zeros(480, 640, CV_8UC3)
        if (skipFrames < 10) {
            return trajectoryImg
        }
        Log.d(TAG, "Drawing trajectory")
        try {
//            Log.d(TAG, "Draw 1")
            Log.d(
                TAG, "Current pos = ${posHistory.peek().t().dump()}"
//                        + "(${posHistory.peek()[0, 0][0]}, "
////                        + "(${posHistory.peek().get(0, 0)[0]}, "
//                        + "${posHistory.peek()[1, 0][0]}, "
//                        + "${posHistory.peek()[2, 0][0]})"
            )
//            Log.d(TAG, "Draw 2")
            // .minBy is nullable
            val minX: Mat = posHistory.getContents().minBy { it[0, 0][0] } ?: posMat
            val minY: Mat = posHistory.getContents().minBy { it[1, 0][0] } ?: posMat
            val maxX = posHistory.getContents().maxBy { it[0, 0][0] } ?: posMat
            val maxY = posHistory.getContents().maxBy { it[1, 0][0] } ?: posMat

//            Log.d(TAG, "Draw 3")
            val aspectRatio = 640 / 480
            val rangeX = maxX[0, 0][0] - minX[0, 0][0]
            val rangeY = maxY[1, 0][0] - minY[1, 0][0]
            val imgRangeX: Double
            val imgRangeY: Double
            val scale: Double
            if (rangeX / rangeY > aspectRatio) {
                imgRangeX = 640.0
                imgRangeY = rangeY / rangeX * imgRangeX
                scale = imgRangeX / rangeX
            } else {
                imgRangeY = 480.0
                imgRangeX = rangeX / rangeY * imgRangeY
                scale = imgRangeY / rangeY
            }
//            Log.d(TAG, "Draw 4")
            val offsetX = -scale * (minX[0, 0][0])
            val offsetY = -scale * (minY[1, 0][0])
            for (pos in posHistory.getContents()) {
//                val ptXPrev = posHistory.peek(1)[0, 0][0] * scale + offsetX
//                val ptYPrev = posHistory.peek(1)[1, 0][0] * scale + offsetY

//                val ptX = pos[0, 0][0] * scale + offsetX
//                val ptY = pos[1, 0][0] * scale + offsetY
//                Imgproc.circle(
//                    trajectoryImg,
//                    Point(ptX, ptY),
//                    5,
//                    Scalar(255.0, 255.0, 255.0),
//                    2
//                )
                Imgproc.circle(
                    trajectoryImg,
                    Point(pos[0,0][0], pos[0,0][0]),
                    10,
                    Scalar(100.0, 255.0, 100.0),
                    5
                )
            }
        } catch (e: NullPointerException) {
            Log.d(TAG, "NullPointerException ignored in drawTrajectory()")
        }

        return trajectoryImg
    }


//    /**
//     *
//     */
//    private fun MutableList<Mat>.toMatOfPoint3(): MatOfPoint3 {
//        val pointList = mutableListOf<Point3>()
//        for (pos in this) {
//            pointList.add(Point3(pos[0, 0][0], pos[1, 0][0], pos[2, 0][0]))
//        }
//        return MatOfPoint3(*pointList.toTypedArray())
//    }
}