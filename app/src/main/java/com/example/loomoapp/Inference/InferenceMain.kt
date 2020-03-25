package com.example.loomoapp.Inference

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.example.loomoapp.Inference.Classifier.Recognition
import com.example.loomoapp.Inference.env.ImageUtils
import com.example.loomoapp.Inference.tracking.MultiBoxTracker
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.Companion.FISHEYE_WIDTH
import com.example.loomoapp.LoopedThread
import java.util.*
import kotlin.math.log

//internal class InferenceMain(private val handlerThread: LoopedThread) : Service() {
class InferenceMain : Service() {
    private val TAG = "InferenceClass"

    public companion object{
        const val YOLO_MODEL_FILE       = "file:///android_asset/yolov2-tiny.pb"
        const val YOLO_INPUT_SIZE       = 416
        const val YOLO_INPUT_NAME       = "input"
        const val YOLO_OUTPUT_NAMES     = "output"
        const val YOLO_BLOCK_SIZE       = 32 // TODO: 23.03.2020 Not 100% sure what this does
        private const val MINIMUM_CONFIDENCE    = 0.25f
        private const val SENSOR_ORIENTATION    = 0
    }

    //Variables
    private lateinit var handlerThread:LoopedThread

    private lateinit var detector: Classifier
    private lateinit var tracker: MultiBoxTracker

    private var lastProcessingTimeMs: Long = 0
    private var timestamp: Long = 0

//    private lateinit var fishEyeImage: Bitmap
//    private lateinit var inferenceImage : Bitmap
//    private lateinit var detectionImage : Bitmap
    private var fishEyeImage: Bitmap = Bitmap.createBitmap(FISHEYE_WIDTH,FISHEYE_HEIGHT,Bitmap.Config.ARGB_8888)
    private var inferenceImage : Bitmap = Bitmap.createBitmap(YOLO_INPUT_SIZE,YOLO_INPUT_SIZE,Bitmap.Config.ARGB_8888)
    private var detectionImage : Bitmap = Bitmap.createBitmap(FISHEYE_WIDTH,FISHEYE_HEIGHT,Bitmap.Config.ARGB_8888)

    private var frameToCropTransform: Matrix = Matrix()
    private var cropToFrameTransform: Matrix = Matrix()

    private var runningDetection: Boolean = false
    private var runningInference: Boolean = false

    //Service management
    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    fun setHandlerThread(handlerThread_: LoopedThread){
        handlerThread = handlerThread_
    }

    fun init(context:Context) {
        Log.d(TAG, "creating the yolo detector");

        detector= TensorFlowYoloDetector.create(
            context.assets, //loads the protobuf file
            YOLO_MODEL_FILE,
            YOLO_INPUT_SIZE,
            YOLO_INPUT_NAME,
            YOLO_OUTPUT_NAMES,
            YOLO_BLOCK_SIZE
        )

//        tracker = MultiBoxTracker(context)

        frameToCropTransform = ImageUtils.getTransformationMatrix(
            FISHEYE_WIDTH, FISHEYE_HEIGHT,
            YOLO_INPUT_SIZE, YOLO_INPUT_SIZE,
            SENSOR_ORIENTATION,true
        )
        frameToCropTransform.invert(cropToFrameTransform)

    }

    //Runnables
    inner class RunInference(private val img:Bitmap, private val yoloInputSize:Int): Runnable{

        override fun run() {
            val scaledImg = Bitmap.createScaledBitmap(img, yoloInputSize, yoloInputSize, false)
//            Log.d("Running detection on image $currTimestamp")
            val startTime = SystemClock.uptimeMillis()
            val results = detector.recognizeImage(scaledImg)
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime

            detectionImage = scaledImg
            val canvas = Canvas(detectionImage)
            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.0f

            val mappedRecognitions: MutableList<Recognition> =
                LinkedList()

            for (result in results) {
                val location = result.location
                if (location != null && result.confidence >= MINIMUM_CONFIDENCE) {
                    canvas.drawRect(location, paint)
                    cropToFrameTransform.mapRect(location)
                    result.location = location
                    mappedRecognitions.add(result)
                }
            }

//            tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp)
//            trackingOverlay.postInvalidate()

//            requestRender() //
            Log.d(TAG, "inference complete");
            runningInference = false
        }
    }

    //Functions

    fun newFrame(img:Bitmap){
//    fun newFrame(array: ByteArray, img:Bitmap){
        ++timestamp // TODO: 24.03.2020 Not sure what this does. Keeping until this works, then evaluate if this is needed.
        val currTimestamp: Long = timestamp
//        val originalLuminance: ByteArray = array
//        tracker.onFrame(
//            FISHEYE_WIDTH,
//            FISHEYE_HEIGHT,
//            FISHEYE_WIDTH,
//            SENSOR_ORIENTATION,
//            originalLuminance,
//            timestamp
//        )
//        fishEyeImage = img
        if (!runningInference){
            Log.d(TAG, "sending image to inference runnable");
            runningInference = true
            handlerThread.handler.post(RunInference(img, YOLO_INPUT_SIZE))
            //TODO: 24.03.2020 render new detection instead of object tracking

        }else{
//            Log.d(TAG, "inference running, skipping frame");
            // TODO: 24.03.2020 run object tracking while inference is running in background
        }
    }

    fun updateImage(img: Bitmap){
//        Log.d(TAG, "updated image through runnable: ");
        inferenceImage = img
    }

    fun getFrame():Bitmap{
        return detectionImage
    }


    fun newInference(){
        // TODO: 23.03.2020 Trigger inference when previous inference cycle complete

    }

}