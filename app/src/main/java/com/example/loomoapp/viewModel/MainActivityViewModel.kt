package com.example.loomoapp.viewModel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.opencv.core.Mat

open class MainActivityViewModel(app: Application) : AndroidViewModel(app) {
    private val TAG = "ViewModel"
    //Variables

    init {
        Log.i(TAG, "ViewModel created")
    }
}
