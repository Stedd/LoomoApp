package com.example.loomoapp.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class ViewModel(app:Application):AndroidViewModel(app){


    private  val viewModelJob = SupervisorJob()
    val text=  MutableLiveData<String>()

    init {
        Log.i("asd", "ViewModel created")
        text.value = "Service not started yet"
    }



}