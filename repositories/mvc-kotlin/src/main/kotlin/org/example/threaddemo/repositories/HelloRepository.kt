package org.example.threaddemo.repositories

import org.springframework.stereotype.Repository

@Repository
class HelloRepository {
    fun count(): Int {
        return 1
    }
}
