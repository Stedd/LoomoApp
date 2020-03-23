package com.example.loomoapp.Inference

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import com.example.loomoapp.LoopedThread

class InferenceMain (
    private val handlerThread: LoopedThread
    ):
    Service(){
    private val TAG = "InferenceClass"

    companion object{
        private const val YOLO_MODEL_FILE   = "file:///android_asset/yolov2-tiny.pb" // TODO: 23.03.2020 Create the asset folder, add neural net
        private const val YOLO_INPUT_SIZE   = 480
        private const val YOLO_INPUT_NAME   = "input"
        private const val YOLO_OUTPUT_NAMES = "output"
        private const val YOLO_BLOCK_SIZE   = 32 // TODO: 23.03.2020 Not 100% sure what this does
    }

    //Variables
    private var computingDetection: Boolean = false





    //Functions
    // TODO: 23.03.2020 Trigger inference when previous inference cycle complete
    // TODO: 23.03.2020 Trigger from newframe to run object tracker on each available frame, post runnable in the observer in Loom realsense?

    fun newFrame(img:Bitmap){
//        handlerThread.handler.post() \\process new frame
    }

    fun newInference(){

    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

}