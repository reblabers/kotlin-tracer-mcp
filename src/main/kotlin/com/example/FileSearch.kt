package com.example

import kotlinx.serialization.Serializable
import java.util.regex.PatternSyntaxException

@Serializable
data class FileSearchRequest(
    // 正規表現パターン（nullの場合は全件取得）
    val pattern: String? = null,
    // ページングオフセット
    val offset: Int = 0,
    // 1ページあたりの件数
    val limit: Int = 50,
    // 最大結果数
    val maxResults: Int = 100,
    // true: フルパス, false: ファイル名のみ
    val fullPath: Boolean = false,
)

@Serializable
data class FileSearchResult(
    // ファイル名またはパスのリスト
    val files: List<String>,
    // 検索条件に一致する総件数
    val totalCount: Int,
    // 次のページのoffset（次のページがない場合はnull）
    val nextOffset: Int?,
)

class FileSearcher(
    private val resources: Resources,
) {
    /**
     * プロジェクト内のファイルを検索します
     */
    fun searchFiles(request: FileSearchRequest): FileSearchResult {
        // ファイル一覧を取得し、一貫した順序でソート
        var files = resources.getFileList().sorted()

        // 正規表現パターンでフィルタリング
        if (request.pattern != null) {
            val regex =
                try {
                    request.pattern.toRegex(RegexOption.IGNORE_CASE)
                } catch (e: PatternSyntaxException) {
                    throw IllegalArgumentException("Invalid regex pattern: ${request.pattern}", e)
                }
            files = files.filter { regex.containsMatchIn(it) }
        }

        // maxResultsの適用
        val maxResults = request.maxResults
        val totalCount = minOf(files.size, maxResults)
        files = files.take(maxResults)

        // ページング処理
        val limit = request.limit
        val start = request.offset
        val end = minOf(start + limit, files.size)

        // パス形式の調整
        val resultFiles =
            files.subList(start, end).map { path ->
                if (request.fullPath) path else path.split("/").last()
            }

        // 次のページのoffsetを計算
        val nextOffset = if (end < totalCount) end else null

        return FileSearchResult(
            files = resultFiles,
            totalCount = totalCount,
            nextOffset = nextOffset,
        )
    }
}
