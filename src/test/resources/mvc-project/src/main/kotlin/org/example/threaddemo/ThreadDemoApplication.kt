package org.example.threaddemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ThreadDemoApplication

fun main(args: Array<String>) {
	runApplication<ThreadDemoApplication>(*args)
}
