package com.example.loomoapp.Inference

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class InferenceMain :Service(){
    private val TAG = "InferenceClass"



    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

}