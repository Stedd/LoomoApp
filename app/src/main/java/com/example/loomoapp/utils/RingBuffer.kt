package com.example.loomoapp.utils

class RingBuffer<T>(val maxSize: Int = 10, val allowOverwrite: Boolean = false) {
    val array = mutableListOf<T?>().apply {
        for (index in 0 until maxSize) {
            add(null)
        }
    }

    var head = 0        // Head: 'oldest' entry (read index)
    var tail = 0        // Tail: 'newest' entry (write index)
    var capacity = 0    // N.o. items in the queue

    fun clear() {
        head = 0
        tail = 0
        capacity = 0
    }

    fun enqueue(item: T): RingBuffer<T> {
        if (capacity == maxSize) {
            if (allowOverwrite) {
                head = (head + 1) % maxSize
            } else {
                throw OverflowException("Queue is full, can't add $item")
            }
        } else {
            ++capacity
        }

        array[tail] = item
        tail = (tail + 1) % maxSize

        return this
    }

    fun dequeue(): T? {
        if (capacity == 0) {
            throw UnderflowException("Queue is empty, can't dequeue()")
        }

        val item = array[head]
        head = (head + 1) % maxSize

        return item
    }

    fun peek(): T? = array[head]
    fun peek(tailOffset: Int): T? {
        if (tailOffset > capacity) tailOffset == capacity
        val index = if (tailOffset < tail) {
            tail - tailOffset
        } else {
            maxSize - (tailOffset - tail)
        }
        return array[index]
    }

    fun contents(): MutableList<T?> {
        return mutableListOf<T?>().apply {
            var itemCount = capacity
            var readIndex = head
            while (itemCount > 0) {
                add(array[readIndex])
                readIndex = (readIndex + 1) % maxSize
                itemCount--
            }
        }
    }
}

class OverflowException(msg: String) : RuntimeException(msg)
class UnderflowException(msg: String) : RuntimeException(msg)