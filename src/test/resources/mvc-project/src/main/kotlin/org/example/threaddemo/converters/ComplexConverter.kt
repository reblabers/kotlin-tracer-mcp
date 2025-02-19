package org.example.threaddemo.converters

fun rootFun(a: Int): Int = a * a

fun rootFun(a: Int, b: Int): Int = a * b

class ComplexConverter(
    private val scale: Int,
) {
    fun convert(value: Int): Int {
        return value * scale
    }

    fun convertGeneric(input: List<String>): List<Int> {
        return input.map { it.length * scale }
    }

    companion object {
        fun default(): ComplexConverter {
            return ComplexConverter(10)
        }
    }
}

enum class MultiplyScale(val scale: Int) {
    DEFAULT(10),
    ;

    fun calculate(value: Int): Int {
        return value * scale
    }
}
