package org.example.threaddemo.services

import org.example.threaddemo.converters.ComplexConverter
import org.example.threaddemo.converters.MultiplyScale
import org.example.threaddemo.repositories.HelloRepository
import org.example.threaddemo.repositories.WorldRepository
import org.springframework.stereotype.Service

fun rootFun(a: Int): Int = a * a

fun rootFun(a: Int, b: Int): Int = a * b

fun <T> List<T>.toHashCode(): Int = this.sumOf { it.hashCode() }

@Service
class ComplexService(
    private val helloRepository: HelloRepository,
    private val worldRepository: WorldRepository,
) {
    class InnerClass {
        companion object {
            fun one(): Int {
                return 1
            }

            fun one(name: String): Int {
                return 1
            }
        }
    }

    data class ComplexResult(val answer: String)

    fun exec(name: String): ComplexResult {
        val hash = listOf(name, String(CharArray(3))).toHashCode()
        val c1 = helloRepository.count() * InnerClass.one()
        val c2 = privateMethod() * InnerClass.one("a")
        val answers = listOf(rootFun(ComplexConverter.default().convert(MultiplyScale.DEFAULT.calculate(hash + c1 + c2))))
        return toResult(name, answers)
    }

    private fun toResult(name: String, answers: List<Int>): ComplexResult = ComplexResult("$name: ${answers.first()}")

    private fun privateMethod(): Int {
        return worldRepository.count() * InnerClass.one()
    }
}
