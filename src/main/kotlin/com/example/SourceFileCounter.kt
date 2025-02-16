package com.example

import kotlinx.serialization.Serializable

@Serializable
data class GetTotalSourceFilesResult(val size: Int)

class SourceFileCounter(
    private val resources: Resources,
) {
    fun getTotalSourceFiles(): GetTotalSourceFilesResult {
        return GetTotalSourceFilesResult(resources.allSources().files.size)
    }
}
