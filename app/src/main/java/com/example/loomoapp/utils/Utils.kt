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

fun ByteBuffer.toShortArray(bigEndian: Boolean = true): ShortArray {
    val bytesInBuffer = this.remaining()
    val tmpArr = if (bigEndian) {
        ShortArray(bytesInBuffer/2) {
            ((this.get().toInt() shl 8) or this.get().toInt()).toShort()
        }
    } else {
        ShortArray(bytesInBuffer/2) {
            (this.get().toInt() or (this.get().toInt() shl 8)).toShort()
        }
    }
    this.rewind()
    return tmpArr
}