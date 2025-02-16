package org.example.threaddemo.services

import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.measureTime

@Service
class HelloService {
    fun sleep(duration: Duration): String {
        val elapsedTime = wait(duration)
        return "Slept: $elapsedTime"
    }

    fun heavyWork(work: Long): String {
        val initialSeed = System.currentTimeMillis() % 4294967296
        val (seed, elapsedTime) = lcg(initialSeed, work)
        return "Worked $seed: $elapsedTime"
    }

    fun heavyWorkWithMemory(work: Long): String {
        val initialSeed = System.currentTimeMillis() % 4294967296
        val (seed, elapsedTime) = lcgWithMemory(initialSeed, work)
        return "Worked(w/m) $seed: $elapsedTime"
    }

    fun hybrid(duration: Duration, work: Long, count: Long): String {
        var seed = System.currentTimeMillis() % 4294967296
        var sleepElapsedTime = Duration.ZERO
        var workElapsedTime = Duration.ZERO
        val elapsedTime = measureTime {
            repeat(count.toInt()) {
                sleepElapsedTime += wait(duration)
                val (tempSeed, tempElapsedTime) = lcg(seed, work)
                seed = tempSeed
                workElapsedTime += tempElapsedTime
            }
        }
        return "Hybrid $seed: $elapsedTime ($sleepElapsedTime, $workElapsedTime)"
    }

    fun hybridWithMemory(duration: Duration, work: Long, count: Long): String {
        var seed = System.currentTimeMillis() % 4294967296
        var sleepElapsedTime = Duration.ZERO
        var workElapsedTime = Duration.ZERO
        val elapsedTime = measureTime {
            repeat(count.toInt()) {
                sleepElapsedTime += wait(duration)
                val (tempSeed, tempElapsedTime) = lcgWithMemory(seed, work)
                seed = tempSeed
                workElapsedTime += tempElapsedTime
            }
        }
        return "Hybrid(w/m) $seed: $elapsedTime ($sleepElapsedTime, $workElapsedTime)"
    }

    private fun wait(duration: Duration): Duration = measureTime {
        Thread.sleep(duration.inWholeMilliseconds)
    }

    private fun lcg(seed: Long, work: Long): Pair<Long, Duration> {
        var seed: Long = seed

        // Linear Congruential Generator (LCG) for simulating heavy work
        val a = 1664525
        val c = 1013904223
        val m = 4294967296 // 2^32
        val elapsedTime = measureTime {
            (0 until work).forEach { _ ->
                seed = (a * seed + c) % m
            }
        }
        return seed to elapsedTime
    }

    private fun lcgWithMemory(seed: Long, work: Long): Pair<Long, Duration> {
        var seed: Long = seed

        // Linear Congruential Generator (LCG) for simulating heavy work
        val a = 1664525
        val c = 1013904223
        val m = 4294967296 // 2^32
        var seeds = mutableListOf<Long>()
        val elapsedTime = measureTime {
            (0 until work).forEach { _ ->
                seed = (a * seed + c) % m
                seeds.add(seed)
            }
        }
        seed += seeds.size.toLong()
        return seed to elapsedTime
    }
}
