package com.example.loomoapp.viewModel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.loomoapp.TAG

open class MainActivityViewModel : ViewModel() {

    //Variables

    val head = MutableLiveData<Head?>()
    val velocity = MutableLiveData<Velocity?>()
    val position = MutableLiveData<Position?>()
    val positionArray = MutableLiveData<PositionArray>()
    val speak = MutableLiveData<Speak?>()
    val volume = MutableLiveData<Volume?>()
    val endableDrive = MutableLiveData<EnableDrive?>()
    val headLightNotification = MutableLiveData<Int>()

    val visionIsActive = MutableLiveData<Boolean>()
    val activeStreams = MutableLiveData<EnableVision>()

    val realSenseColorImage = MutableLiveData<Bitmap>()
    val realSenseDepthImage = MutableLiveData<Bitmap>()

    val colorLargeBitArray = MutableLiveData<ByteArray>()
    val colorSmallBitArray = MutableLiveData<ByteArray>()
    val colorDepthBitArray = MutableLiveData<ByteArray>()

    var text = MutableLiveData<String>()

    init {

        Log.i(TAG, "ViewModel created")
        text.value = "Service not started yet"
    }
}

data class EnableDrive( //
    val drive : Boolean
//    val act: String = Action.ENABLE_DRIVE
)

data class EnableVision( //
    val depth : Boolean,
    val color : Boolean,
    val colorSmall : Boolean
//    val act: String = Action.ENABLE_VISION
)

data class Head( //
    var pitch :Float, // Head pitch
    var yaw : Float, // Head Yaw
    var li : Int? = null // Head light mode 0-13
//    var mode : Int = Action.HEAD_SET_SMOOTH,
//    val act : String = Action.HEAD
    )

data class Velocity( //
    val v : Float, // Linear Velocity
    val av : Float // Angular velocity
//    val act: String = Action.VELOCITY
)

data class Position(
    val x : Float, // X direction absolute movement
    val y : Float, // Y direction absolute movement
    var th: Float? = null,
    var add: Boolean = false,
    var vls: Boolean = false
//    val act: String = Action.POSITION
)

data class PositionArray(
    val x: FloatArray,
    val y: FloatArray,
    var th: FloatArray? = null,
    var add: Boolean = false,
    var vls: Boolean = false
//    val act: String = Action.POSITION_ARRAY
)

data class Speak(
    val length : Int, // Length Of string to come
    var pitch : Float = 1.0F, // Pitch of the voice
    var que : Int = 0, // Should the speaker be qued
    var string: String = ""
//    val act : String = Action.SPEAK
)

data class Volume(
    val v : Double
//    val act: String = Action.VOLUME
)

data class SensSurroundings(
    val IR_Left : Int,
    val IR_Right : Int,
    val UltraSonic : Int
//    val label : String = DataResponce.SURROUNDINGS
    )

data class SensWheelSpeed(
    val SpeedLeft : Float,
    val SpeedRight : Float
//    val label : String = DataResponce.WHEEL_SPEED
)

data class SensHeadPoseWorld(
    val pitch : Float,
    val roll : Float,
    val yaw : Float
//    val label : String = DataResponce.HEAD_WORLD
)

data class SensHeadPoseJoint(
    val pitch : Float,
    val roll : Float,
    val yaw : Float
//    val label : String = DataResponce.HEAD_JOINT
)

data class SensBaseImu(
    val pitch : Float,
    val roll : Float,
    val yaw : Float
//    val label : String = DataResponce.BASE_IMU
)

data class SensBaseTick(
    val left : Int,
    val right : Int
//    val label : String = DataResponce.BASE_TICK
)

data class SensPose2D(
    val x : Float,
    val y : Float,
    val theta : Float,
    val linearVelocity : Float,
    val angularVelocity: Float
//    val label : String = DataResponce.POSE2D
)

data class ImageResponse(
//    var type : String = DataResponce.IMAGE_TYPE_COLOR_SMALL,
    var size : Int = 0,
    var width : Int = 0,
    var height : Int = 0
//    val label : String = DataResponce.IMAGE
) {
//    init {
//        if (type == DataResponce.IMAGE_TYPE_COLOR) {
//            width = LoomoRealSense.COLOR_WIDTH
//            height = LoomoRealSense.COLOR_HEIGHT
//        } else {
//            width = LoomoRealSense.SMALL_COLOR_WIDTH
//            height = LoomoRealSense.SMALL_COLOR_HEIGHT
//        }
//    }
}