package com.example.loomoapp

class Sender :ThreadLoop(){
    override var interval: Long = 250

    override fun main() {
        // Obtain sensor data

        //Pack into JSON structure

        //Send on demand..... Better to use coroutine with request listener?
    }
}