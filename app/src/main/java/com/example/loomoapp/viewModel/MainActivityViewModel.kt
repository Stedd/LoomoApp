package com.example.loomoapp.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

//class MainActivityViewModel(app:Application):AndroidViewModel(app){
class MainActivityViewModel : ViewModel() {

    //Variables
    var text = MutableLiveData<String>()
//    var index = MutableLiveData<Int>()

    //Initialize
    init {
//        viewModelScope.launch(Dispatchers.IO){
//            Log.i("asd", "Coroutine created")
//        }
        Log.i("asd", "ViewModel created")
//        index.value = 0
        text.value = "Service not started yet"
    }

    //Functions
//    fun startCoroutine() {
//        viewModelScope.launch(Dispatchers.IO) {
//            Log.i("asd", "Coroutine created")
//        }
//    }

//    private val viewModelJob = SupervisorJob()

//    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

}