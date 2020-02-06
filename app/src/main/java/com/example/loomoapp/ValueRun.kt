package com.example.loomoapp

import android.os.Process
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel

class IncrementValueRunnable:Runnable{

//    private var viewModel: MainActivityViewModel = ViewModelProvider(MainActivity::class.java)
//        .get(MainActivityViewModel::class.java)
//
    var index = 0
    override fun run() {


        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (true){
            index++
            Log.i("asd", "Index: $index")
//            viewModel.text.value = index.toString()
        }
    }

}