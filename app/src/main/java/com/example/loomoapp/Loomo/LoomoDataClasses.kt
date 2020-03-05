package com.example.loomoapp.Loomo

    data class EnableDrive( //
        val drive : Boolean
    )

    data class EnableVision( //
        val depth : Boolean,
        val color : Boolean,
        val colorSmall : Boolean
    )

    data class Head( //
        var pitch :Float, // Head pitch
        var yaw : Float, // Head Yaw
        var li : Int? = null // Head light mode 0-13
    )

    data class Velocity( //
        val v : Float, // Linear Velocity
        val av : Float // Angular velocity
    )

    data class Position(
        val x : Float, // X direction absolute movement
        val y : Float, // Y direction absolute movement
        var th: Float? = null,
        var add: Boolean = false,
        var vls: Boolean = false
    )

    data class PositionArray(
        val x: FloatArray,
        val y: FloatArray,
        var th: FloatArray? = null,
        var add: Boolean = false,
        var vls: Boolean = false
    )

    data class Speak(
        val length : Int, // Length Of string to come
        var pitch : Float = 1.0F, // Pitch of the voice
        var que : Int = 0, // Should the speaker be qued
        var string: String = ""
    )

    data class Volume(
        val v : Double
    )

    data class SensSurroundings(
        val IR_Left : Int,
        val IR_Right : Int,
        val UltraSonic : Int
    )

    data class SensWheelSpeed(
        val SpeedLeft : Float,
        val SpeedRight : Float
    )

    data class SensHeadPoseWorld(
        val pitch : Float,
        val roll : Float,
        val yaw : Float
    )

    data class SensHeadPoseJoint(
        val pitch : Float,
        val roll : Float,
        val yaw : Float
    )

    data class SensBaseImu(
        val pitch : Float,
        val roll : Float,
        val yaw : Float
    )

    data class SensBaseTick(
        val left : Int,
        val right : Int
    )

    data class SensPose2D(
        val x : Float,
        val y : Float,
        val theta : Float,
        val linearVelocity : Float,
        val angularVelocity: Float
    )

    data class ImageResponse(
        var size : Int = 0,
        var width : Int = 0,
        var height : Int = 0
    )
//}