package com.example.loomoapp.utils


//TODO: Use something other than null-assertion (!!) in 'peek()',
// 'peekHead()' and 'getContents()'. The two first will functions will
// cause a NPE if they are called on an empty buffer. They will also probably
// do the same if the template-type 'T' is nullable
class RingBuffer<T>(val maxSize: Int = 10, val allowOverwrite: Boolean = false) {
    private val array = mutableListOf<T?>().apply {
        for (index in 0 until maxSize) {
            add(null)
        }
    }

    var head = 0        // Head: 'oldest' entry (read index)
        private set
    var tail = 0        // Tail: 'newest' entry (write index)
        private set
    var itemsInQueue = 0    // N.o. items in the queue
        private set

    fun clear() {
        head = 0
        tail = 0
        itemsInQueue = 0
    }

    fun enqueue(item: T): RingBuffer<T> {
        if (itemsInQueue != 0) {
            tail = (tail + 1) % maxSize
        }
        if (itemsInQueue == maxSize) {
            if (allowOverwrite) {
                head = (head + 1) % maxSize
            } else {
                throw OverflowException("Queue is full, can't add $item")
            }
        } else {
            ++itemsInQueue
        }

        array[tail] = item

        return this
    }

    fun dequeue(): T? {
        if (itemsInQueue == 0) {
            throw UnderflowException("Queue is empty, can't dequeue()")
        }

        val item = array[head]
        head = (head + 1) % maxSize

        return item
    }

    fun peek(tailOffset: Int = 0): T {
        var offset = tailOffset
        if (offset > itemsInQueue) {
            offset = itemsInQueue
        }
        val index = if (offset <= tail) {
            tail - offset
        } else {
            maxSize - (offset - tail)
        }
        return array[index]!!
    }
    fun peekHead(): T = array[head]!!

    fun getContents(): MutableList<T> {
        return mutableListOf<T>().apply {
            var itemCount = itemsInQueue
            var readIndex = head
            while (itemCount > 0) {
                add(array[readIndex]!!)
                readIndex = (readIndex + 1) % maxSize
                itemCount--
            }
        }
    }

    fun isEmpty() = itemsInQueue == 0
    fun freeSpace() = maxSize - itemsInQueue
    operator fun get(index: Int): T? {
        return array[index]
    }
}

class OverflowException(msg: String) : RuntimeException(msg)
class UnderflowException(msg: String) : RuntimeException(msg)