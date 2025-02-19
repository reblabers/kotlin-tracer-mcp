package org.example.threaddemo.controllers

import org.example.threaddemo.services.OpService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RestController
@RequestMapping("/op")
class OpController(
    private val opService: OpService,
) {
    @GetMapping("/multiply")
    fun multiply(): String {
        return opService.multiply().toString()
    }

    @GetMapping("/multi")
    @ResponseBody
    fun multiAnnotatedMethod(): String {
        return "Multiple annotations"
    }
}
