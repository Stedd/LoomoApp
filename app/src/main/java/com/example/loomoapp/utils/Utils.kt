package com.example.loomoapp.utils

import java.nio.ByteBuffer

fun ByteBuffer.copy(): ByteBuffer {
    val copy = ByteBuffer.allocate(this.capacity())
    this.rewind()
    copy.put(this)
    this.rewind()
    copy.flip()
    return copy
}

fun ByteBuffer.toByteArray(): ByteArray {
    val bytesInBuffer = this.remaining()
    val tmpArr = ByteArray(bytesInBuffer) { this.get() }
    this.rewind()
    return tmpArr
}