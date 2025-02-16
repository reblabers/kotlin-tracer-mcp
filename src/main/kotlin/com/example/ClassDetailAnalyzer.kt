package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtClassOrObject

@Serializable
data class ClassDetailsResult(
    val results: List<ClassDetailResult>,
    val failedQualifiedNames: List<String>,
)

@Serializable
data class ClassDetailResult(
    val qualifiedName: String,
    val sourceCode: String,
)

class ClassDetailAnalyzer(
    private val resources: Resources,
) {
    fun getClassDetails(qualifiedClassNames: List<String>): ClassDetailsResult {
        val successResults = mutableListOf<ClassDetailResult>()
        val failedQualifiedNames = mutableListOf<String>()

        qualifiedClassNames.forEach { qualifiedClassName ->
            try {
                val result = getClassDetail(qualifiedClassName)
                successResults.add(result)
            } catch (_: IllegalArgumentException) {
                failedQualifiedNames.add(qualifiedClassName)
            }
        }

        return ClassDetailsResult(successResults, failedQualifiedNames)
    }

    fun getClassDetail(qualifiedClassName: String): ClassDetailResult {
        val sourceFiles = resources.allSources()

        for (sourceFile in sourceFiles.files) {
            val ktFile = sourceFile.readonly

            ktFile.declarations.filterIsInstance<KtClassOrObject>().forEach { ktClassOrObject ->
                if (ktClassOrObject.fqName?.asString() == qualifiedClassName) {
                    return createClassDetailResult(ktClassOrObject)
                }
            }
        }

        throw IllegalArgumentException("Class not found: $qualifiedClassName")
    }

    private fun createClassDetailResult(ktClassOrObject: KtClassOrObject): ClassDetailResult {
        return ClassDetailResult(
            qualifiedName = ktClassOrObject.fqName?.asString() ?: "",
            sourceCode = ktClassOrObject.text ?: "",
        )
    }
}
