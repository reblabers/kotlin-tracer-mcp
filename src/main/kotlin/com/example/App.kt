package com.example

import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

fun main() {
    val bufferedInput = System.`in`.asSource().buffered()
    val printOutput = System.out.asSink().buffered()

    val server = configureServer()
    val transport = StdioServerTransport(bufferedInput, printOutput)

    runBlocking {
        server.connect(transport)

        val done = Job()
        server.onCloseCallback = {
            done.complete()
        }
        done.join()
    }
}
