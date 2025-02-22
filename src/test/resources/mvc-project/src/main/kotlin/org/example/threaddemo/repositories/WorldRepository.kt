package org.example.threaddemo.repositories

import org.springframework.stereotype.Repository

@Repository
class WorldRepository {
    var weight = 2

    fun count(): Int {
        return weight
    }
}
