package com.example.loomoapp.utils

class Vector() {
    var x = 0f
    var y = 0f

    constructor(x: Float, y: Float) : this() {
        this.x = x
        this.y = y
    }

    fun set(a: Vector) {
        x = a.x
        y = a.y
    }

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    //Multiply
    fun mult(a: Float){
        x *= a
        y *= a
    }

    fun mult(a: Vector, b: Float): Vector {
        return Vector(a.x * b, a.y * b)
    }

    //Addition
    fun add(a: Vector) {
        x += a.x
        y += a.y
    }

    fun add(a: Vector, b: Vector): Vector {
        return Vector(a.x + b.x, a.y + b.y)
    }

    //Subtraction
    fun sub(a: Vector) {
        x -= a.x
        y -= a.y
    }
    fun sub(a: Vector, b: Vector): Vector {
        return Vector(a.x - b.x, a.y - b.y)
    }

//    fun angleBetween(a:Vector, b:Vector):Vector{
//        return Vector()
//    }

}