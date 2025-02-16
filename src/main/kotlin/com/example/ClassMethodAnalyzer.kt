package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction

@Serializable
data class ClassMethodsResult(
    val className: String,
    // 完全修飾名
    val qualifiedName: String,
    val methods: List<MethodInfo>,
)

sealed class ClassSearchResult {
    data class Found(val qualifiedName: String) : ClassSearchResult()

    data class MultipleFound(
        // 候補となるクラスの完全修飾名リスト
        val candidates: List<String>,
    ) : ClassSearchResult()

    object NotFound : ClassSearchResult()
}

class ClassMethodAnalyzer(
    private val resources: Resources,
    private val classNameSearcher: ClassNameSearcher,
) {
    /**
     * クラス名から対象のKtClassを検索します。
     *
     * @param className クラス名（単純名または完全修飾名）
     * @return 検索結果（ClassSearchResult）
     */
    private fun findTargetClass(className: String): ClassSearchResult {
        val searchResult =
            classNameSearcher.searchClassNames(
                ClassNameSearchRequest(
                    pattern = className,
                    offset = 0,
                    limit = 10,
                    maxResults = 10,
                    fullPath = true,
                ),
            )
        val qualifiedClassNames = searchResult.qualifiedClassNames
        return when {
            qualifiedClassNames.isEmpty() -> ClassSearchResult.NotFound
            qualifiedClassNames.size == 1 -> ClassSearchResult.Found(qualifiedClassNames.single())
            else -> ClassSearchResult.MultipleFound(qualifiedClassNames)
        }
    }

    /**
     * 単純名でクラスを検索し、メソッド情報を解析します。
     *
     * @param className クラス名（単純名）
     * @return 解析結果（ClassMethodsResult）。クラスが特定できない場合はエラー情報。
     */
    fun analyzeBySimpleName(className: String): ClassMethodsResult {
        return when (val result = findTargetClass("\\.$className$")) {
            is ClassSearchResult.Found -> analyzeByFqName(result.qualifiedName)
            is ClassSearchResult.MultipleFound -> throw IllegalArgumentException(
                "Multiple classes found with name '$className': ${result.candidates.joinToString(", ")}",
            )
            is ClassSearchResult.NotFound -> throw IllegalArgumentException(
                "Class not found: $className",
            )
        }
    }

    /**
     * 完全修飾名でクラスを検索し、メソッド情報を解析します。
     *
     * @param fqName クラスの完全修飾名
     * @return 解析結果（ClassMethodsResult）。クラスが見つからない場合はエラー情報。
     */
    fun analyzeByFqName(fqName: String): ClassMethodsResult {
        val sourceFiles = resources.allSources()

        // 完全修飾名に一致するクラスを検索
        for (sourceFile in sourceFiles.files) {
            val ktFile = sourceFile.readonly
            val classes = ktFile.classOrInterfaceList()

            for (ktClass in classes) {
                val classQualifiedName = ktClass.fqName?.asString() ?: continue
                if (classQualifiedName == fqName) {
                    return analyzeClass(ktClass)
                }
            }
        }

        throw IllegalArgumentException("Class not found: $fqName")
    }

    /**
     * KtClassからメソッド情報を抽出します。
     */
    private fun analyzeClass(ktClass: KtClass): ClassMethodsResult {
        val methods =
            ktClass.declarations
                .filterIsInstance<KtFunction>()
                .map { function ->
                    MethodInfo.from(function)
                }

        return ClassMethodsResult(
            className = ktClass.name ?: "",
            qualifiedName = ktClass.fqName?.asString() ?: "",
            methods = methods,
        )
    }
}
