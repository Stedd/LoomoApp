package com.example.loomoapp.utils

class Vector() {
    var x_ = 0f
    var y_ = 0f

    constructor(x: Float, y: Float) : this() {
        x_ = x
        y_ = y
    }

    fun set(a: Vector) {
        x_ = a.x_
        y_ = a.y_
    }

    fun set(x: Float, y: Float) {
        x_ = x
        y_ = y
    }

    //Multiply
    fun mult(a: Float){
        x_ *= a
        y_ *= a
    }

    fun mult(a: Vector, b: Float): Vector {
        return Vector(a.x_ * b, a.y_ * b)
    }

    //Addition
    fun add(a: Vector) {
        x_ += a.x_
        y_ += a.y_
    }

    fun add(a: Vector, b: Vector): Vector {
        return Vector(a.x_ + b.x_, a.y_ + b.y_)
    }

    //Subtraction
    fun sub(a: Vector) {
        x_ -= a.x_
        y_ -= a.y_
    }
    fun sub(a: Vector, b: Vector): Vector {
        return Vector(a.x_ - b.x_, a.y_ - b.y_)
    }

//    fun angleBetween(a:Vector, b:Vector):Vector{
//        return Vector()
//    }

}