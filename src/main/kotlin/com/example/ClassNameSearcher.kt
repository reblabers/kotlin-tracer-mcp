package com.example

import kotlinx.serialization.Serializable
import java.util.regex.PatternSyntaxException

@Serializable
data class ClassNameSearchRequest(
    val pattern: String? = null,
    val offset: Int = 0,
    val limit: Int = 50,
    val maxResults: Int = 100,
    val fullPath: Boolean = false,
)

@Serializable
data class ClassNameSearchResult(
    val qualifiedClassNames: List<String>,
    // 検索条件に一致する総件数
    val totalCount: Int,
    // 次のページのoffset（次のページがない場合はnull）
    val nextOffset: Int?,
)

class ClassNameSearcher(
    private val resources: Resources,
) {
    /**
     * 指定された検索条件に合致するクラス名を検索します。
     *
     * @param request 検索条件
     * @return 検索結果
     */
    fun searchClassNames(request: ClassNameSearchRequest): ClassNameSearchResult {
        // ソースファイル一覧を取得
        val sourceFiles = resources.allSources()
        val allQualifiedNames = mutableListOf<String>()

        // 正規表現パターンでフィルタリング
        if (request.pattern != null) {
            val regex =
                try {
                    request.pattern.toRegex(RegexOption.IGNORE_CASE)
                } catch (e: PatternSyntaxException) {
                    throw IllegalArgumentException("Invalid regex pattern: ${request.pattern}", e)
                }

            // 各ファイルからクラスを抽出し、パターンに一致するものを検索
            for (sourceFile in sourceFiles.files) {
                val ktFile = sourceFile.readonly
                val classes = ktFile.classOrInterfaceList()

                for (ktClass in classes) {
                    val fqName = ktClass.fqName?.asString() ?: continue
                    // パターンマッチングは常に完全修飾名に対して行う
                    if (regex.containsMatchIn(fqName)) {
                        allQualifiedNames.add(fqName)
                    }
                }
            }
        } else {
            // パターンが指定されていない場合は、すべてのクラスを返す
            for (sourceFile in sourceFiles.files) {
                val ktFile = sourceFile.readonly
                val classes = ktFile.classOrInterfaceList()

                for (ktClass in classes) {
                    val fqName = ktClass.fqName?.asString() ?: continue
                    allQualifiedNames.add(fqName)
                }
            }
        }

        // ソートして結果を返す
        val sortedNames = allQualifiedNames.sorted()

        // maxResultsの制限を適用（総件数の計算用）
        val maxLimitedNames = sortedNames.take(request.maxResults)
        val totalCount = maxLimitedNames.size

        // ページネーションを適用
        val start = request.offset.coerceAtMost(maxLimitedNames.size)
        val end = (start + request.limit).coerceAtMost(maxLimitedNames.size)
        val pagedNames = maxLimitedNames.subList(start, end)

        // パス表示の設定を適用（最後に行う）
        val finalNames =
            if (!request.fullPath) {
                pagedNames.map { it.substringAfterLast('.') }
            } else {
                pagedNames
            }

        // 次のページのoffsetを計算
        val nextOffset = if (end < maxLimitedNames.size) end else null

        return ClassNameSearchResult(
            qualifiedClassNames = finalNames,
            totalCount = totalCount,
            nextOffset = nextOffset,
        )
    }
}
