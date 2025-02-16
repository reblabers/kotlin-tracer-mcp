package org.example.threaddemo.repositories

import org.springframework.stereotype.Repository

@Repository
class WorldRepository {
    fun count(): Int {
        return 2
    }
}
