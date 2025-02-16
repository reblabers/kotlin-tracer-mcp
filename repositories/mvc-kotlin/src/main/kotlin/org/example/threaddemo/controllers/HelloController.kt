package org.example.threaddemo.controllers

import org.example.threaddemo.services.HelloService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RestController
@RequestMapping("/api")
class HelloController(
    private val helloService: HelloService,
) {
    @GetMapping("/processors")
    fun processors(): String {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return "Available processors: $availableProcessors"
    }

    @GetMapping("/sleep")
    fun sleep(ms: Long): String {
        val duration = ms.toDuration(DurationUnit.MILLISECONDS)
        return helloService.sleep(duration)
    }

    @GetMapping("/heavy")
    fun heavy(work: Long, memory: Boolean = false): String {
        return if (memory) {
            helloService.heavyWorkWithMemory(work)
        } else {
            helloService.heavyWork(work)
        }
    }

    @GetMapping("/hybrid")
    fun hybrid(ms: Long, work: Long, count: Long, memory: Boolean = false): String {
        val duration = ms.toDuration(DurationUnit.MILLISECONDS)
        return if (memory) {
            helloService.hybridWithMemory(duration, work, count)
        } else {
            helloService.hybrid(duration, work, count)
        }
    }
}
