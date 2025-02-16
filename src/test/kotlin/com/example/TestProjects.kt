package com.example

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * テストで使用するプロジェクト構成を一元管理するenum
 */
enum class TestProjects(
    val path: Path,
    val jarPath: Path? = null,
) {
    SOURCE_COUNT_PROJECT(Path("src/test/resources/source-count-project")), // 3つのソースファイルを含む基本的なプロジェクト
    MANY_FILES_PROJECT(Path("src/test/resources/many-files-project")), // 7つのソースファイルを含むプロジェクト（main:5 + test:2）
    MVC_PROJECT(
        path = Path("src/test/resources/mvc-project"),
        jarPath = Path("src/test/resources/mvc-project/mvc-project.jar"),
    ), // MVCプロジェクトのテスト用
}
