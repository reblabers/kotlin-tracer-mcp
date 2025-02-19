package org.example.threaddemo.services

import org.example.threaddemo.repositories.HelloRepository
import org.example.threaddemo.repositories.WorldRepository
import org.springframework.stereotype.Service

interface Op {
    fun divide(a: Int): Int = a / 2
}

@Service
class OpService(
    private val helloRepository: HelloRepository,
    private val worldRepository: WorldRepository,
) {
    class InnerClass : Op {
        fun multiply(a: Int): Int {
            return 3 * a
        }
    }

    fun plus(a: Int): Int {
        return a + 1
    }

    fun multiply(): Int {
        val c1 = helloRepository.count()
        val c2 = privateMethod()
        return c1 * c2
    }

    private fun privateMethod(): Int {
        return worldRepository.count()
    }

    suspend fun suspendOperation(): String {
        return "Suspended operation completed"
    }
}
