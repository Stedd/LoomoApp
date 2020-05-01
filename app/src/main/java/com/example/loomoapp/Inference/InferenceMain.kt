package com.example.loomoapp.Inference

/**

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.loomoapp.Inference.Classifier.Recognition
import com.example.loomoapp.Inference.env.ImageUtils
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomoapp.Loomo.LoomoRealSense.FISHEYE_WIDTH
import com.example.loomoapp.utils.LoopedThread
import java.lang.Math.random
import java.util.*


class InferenceMain : Service() {
    private val TAG = "InferenceClass"

    companion object {
        const val YOLO_MODEL_FILE = "file:///android_asset/yolov2-tiny.pb"
        const val YOLO_INPUT_SIZE = 416
        const val YOLO_INPUT_NAME = "input"
        const val YOLO_OUTPUT_NAMES = "output"
        const val YOLO_BLOCK_SIZE = 32 //
        const val MINIMUM_CONFIDENCE = 0.4f
        const val ENABLE_DEBUG = false
        private const val SENSOR_ORIENTATION = 0
    }

    //Variables
    private lateinit var handlerThread: LoopedThread
    private lateinit var uiHandler: Handler

    private lateinit var inferenceImageViewBitmap: MutableLiveData<Bitmap>
    private var inferenceImage: Bitmap = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT,Bitmap.Config.ARGB_8888)
    private var croppedImage: Bitmap = Bitmap.createBitmap(YOLO_INPUT_SIZE, YOLO_INPUT_SIZE,Bitmap.Config.ARGB_8888)
    private var scaledImage: Bitmap = Bitmap.createBitmap(YOLO_INPUT_SIZE, YOLO_INPUT_SIZE,Bitmap.Config.ARGB_8888)

    private lateinit var detector: Classifier

    private var lastProcessingTimeMs: Long = 0

    private var frameToCropTransform: Matrix = Matrix()
    private var cropToFrameTransform: Matrix = Matrix()

    private var runningInference: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    fun setHandlerThread(handlerThread_: LoopedThread) {
        handlerThread = handlerThread_
    }

    fun setMainUIHandler(handler_: Handler) {
        uiHandler = handler_
    }

    fun setInferenceBitmap(inferenceImage_: MutableLiveData<Bitmap>) {
        inferenceImageViewBitmap = inferenceImage_
    }

    fun init(context: Context) {
        Log.d(TAG, "creating the yolo detector");

        detector = TensorFlowYoloDetector.create(
            context.assets, //loads the protobuf file
            YOLO_MODEL_FILE,
            YOLO_INPUT_SIZE,
            YOLO_INPUT_NAME,
            YOLO_OUTPUT_NAMES,
            YOLO_BLOCK_SIZE
        )

        frameToCropTransform = ImageUtils.getTransformationMatrix(
            FISHEYE_WIDTH, FISHEYE_HEIGHT,
            YOLO_INPUT_SIZE, YOLO_INPUT_SIZE,
            SENSOR_ORIENTATION, true
        )
        frameToCropTransform.invert(cropToFrameTransform)
    }

    fun newFrame(img: Bitmap) {
//        Log.d(TAG, "before if:$runningInference");
        if (!runningInference) {
            if (ENABLE_DEBUG) {
            Log.d(TAG, "sending image to inference runnable");
            }
            handlerThread.handler.removeCallbacks(RunInference(img, YOLO_INPUT_SIZE))
            runningInference = true
//            Log.d(TAG, "posting image");
            handlerThread.handler.post(RunInference(img, YOLO_INPUT_SIZE))

        }
    }

    inner class RunInference(private val img: Bitmap, private val yoloInputSize: Int) : Runnable {
        override fun run() {
//            Log.d(TAG, "start of runnable:$runningInference");
            //crop input image
            inferenceImage = img
//            val inputCanvas = Canvas(croppedImage);
//            inputCanvas.drawBitmap(img, frameToCropTransform, null);
            //scale input image
            scaledImage = Bitmap.createScaledBitmap(img, yoloInputSize, yoloInputSize, false)
            val startTime = SystemClock.uptimeMillis()
            val results = detector.recognizeImage(scaledImage)
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime

            val canvas = Canvas(scaledImage)
//            val mappedRecognitions: MutableList<Recognition> = LinkedList()
            val boxPaint = Paint()
            boxPaint.color = Color.RED
            boxPaint.style = Paint.Style.STROKE
            boxPaint.strokeWidth = 1.0f
            val textPaint = Paint()
            textPaint.color = Color.WHITE
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 10F
            for (result in results) {
                val location = result.location
//                cropToFrameTransform.mapRect(location)
                val id = result.title
                val confidence = result.confidence
                if (location != null && result.confidence >= MINIMUM_CONFIDENCE) {
                    canvas.drawRect(location, boxPaint)
                    canvas.drawText(
                        "$id: $confidence",
                        location.centerX(),
                        location.centerY() + (location.height() / 2) - random().toFloat() * 30f,
                        textPaint
                    )
//                    result.location = location
//                    mappedRecognitions.add(result)
                }
            }

            uiHandler.post {
                inferenceImageViewBitmap.value = scaledImage
            }

            runningInference = false
//            Log.d(TAG, "end of runnable:$runningInference");

            if (ENABLE_DEBUG) {
            Log.d(TAG, "inference complete");
            }
        }
    }
}

        */