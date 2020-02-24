package com.example.loomoapp.Loomo

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.loomoapp.TAG
import com.segway.robot.sdk.base.bind.ServiceBinder
import com.segway.robot.sdk.locomotion.sbv.Base

class LoomoBase() {
    private val mBase :Base= Base.getInstance()

    fun bind(context: Context){
        //Bind Base SDK service
        mBase.bindService(context, object : ServiceBinder.BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Base onBind")
            }

            override fun onUnbind(reason: String?) {
                Log.d(TAG, "Base unBind. Reason: $reason")
            }
        })
    }

}