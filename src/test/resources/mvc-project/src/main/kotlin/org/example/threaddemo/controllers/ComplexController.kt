package org.example.threaddemo.controllers

import org.example.threaddemo.services.ComplexService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class ComplexRequest(val name: String)

@RestController
@RequestMapping("/complex")
class ComplexController(
    private val complexService: ComplexService,
) {
    @PostMapping("/exec")
    fun exec(request: ComplexRequest): String {
        return complexService.exec(request.name).answer
    }
}
