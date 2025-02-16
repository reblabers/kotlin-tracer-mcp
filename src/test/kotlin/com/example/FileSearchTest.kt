package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class FileSearchTest : DescribeSpec({
    describe("FileSearch") {
        describe("基本的な検索機能") {
            it("デフォルト設定で全ファイルを検索できる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val result = searcher.searchFiles(FileSearchRequest())

                result.files.shouldNotBeEmpty()
                result.totalCount shouldBe 8 // MANY_FILES_PROJECTには8つのファイルがある（main:5 + test:3）
                result.nextOffset.shouldBeNull() // 全件取得できているのでnextOffsetはnull
            }
        }

        describe("正規表現検索") {
            it("パターンに一致するファイルのみを返す") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = ".*File[2-3]\\.kt$", // File2.ktとFile3.ktにマッチ
                        ),
                    )

                result.files.size shouldBe 3 // main:2 (File2.kt, File3.kt) + test:1 (File2.kt)
                result.files.all { it.endsWith(".kt") }.shouldBeTrue()
            }

            it("フルパスに対してパターンマッチングを行う") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()

                // フルパスでマッチング
                val resultWithPath =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = ".*src/main/kotlin.*File1\\.kt$",
                            fullPath = true,
                        ),
                    )
                resultWithPath.files.size shouldBe 1
                resultWithPath.files.first() shouldContain "src/main/kotlin"
                resultWithPath.files.first() shouldContain "File1.kt"

                // ファイル名のみでもマッチング
                val resultWithFileName =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = "File1\\.kt$",
                            fullPath = false, // フルパス表示はオフだが、マッチングはフルパスに対して行われる
                        ),
                    )
                resultWithFileName.files.size shouldBe 2 // main:1 + test:1
                resultWithFileName.files.all { it == "File1.kt" }.shouldBeTrue()
            }

            describe("パターンマッチングと表示形式の組み合わせ") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()

                it("フルパスでの検索＆フルパスでの表示") {
                    val result =
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = ".*src/main/kotlin.*File1\\.kt$",
                                fullPath = true,
                            ),
                        )
                    result.files.size shouldBe 1
                    result.files.first() shouldContain "src/main/kotlin"
                    result.files.first() shouldContain "File1.kt"
                }

                it("フルパスでの検索＆ファイル名のみ表示") {
                    val result =
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = ".*src/main/kotlin.*File1\\.kt$",
                                fullPath = false,
                            ),
                        )
                    result.files.size shouldBe 1
                    result.files.first() shouldBe "File1.kt"
                }

                it("ファイル名のみでの検索＆ファイル名のみ表示") {
                    val result =
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = "File1\\.kt$",
                                fullPath = false,
                            ),
                        )
                    result.files.size shouldBe 2 // main:1 + test:1
                    result.files.all { it == "File1.kt" }.shouldBeTrue()
                }

                it("ファイル名のみでの検索＆フルパスでの表示") {
                    val result =
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = "File1\\.kt$",
                                fullPath = true,
                            ),
                        )
                    result.files.size shouldBe 2 // main:1 + test:1
                    result.files.any { it.contains("src/main/kotlin") }.shouldBeTrue()
                    result.files.any { it.contains("src/test/kotlin") }.shouldBeTrue()
                    result.files.all { it.contains("File1.kt") }.shouldBeTrue()
                }

                it("パスの一部を含むパターンでの検索") {
                    // src/mainを含むパターンで検索
                    val result =
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = ".*src/main.*File1\\.kt$",
                                fullPath = false,
                            ),
                        )
                    result.files.size shouldBe 1
                    result.files.first() shouldBe "File1.kt"
                }

                it("存在しないパスパターンでは結果が空になる") {
                    val result =
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = ".*non/existent/path.*\\.kt$",
                                fullPath = true,
                            ),
                        )
                    result.files.shouldBeEmpty()
                }
            }
        }

        describe("ページング") {
            it("検索結果が一貫した順序で返される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()

                // 同じ検索を2回実行
                val result1 =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = 5,
                        ),
                    )

                val result2 =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = 5,
                        ),
                    )

                // 結果が同じ順序であることを確認
                result1.files shouldBe result2.files
            }

            it("ページングの結果が一貫した順序で返される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val limit = 2

                // 1ページ目を取得
                val result1 =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = limit,
                            offset = 0,
                        ),
                    )

                // 2ページ目を取得
                val result2 =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = limit,
                            offset = result1.nextOffset!!,
                        ),
                    )

                // 全件を一度に取得
                val fullResult =
                    searcher.searchFiles(
                        FileSearchRequest(),
                    )

                // ページングの結果が全件取得の結果と一致することを確認
                (result1.files + result2.files) shouldBe fullResult.files.take(limit * 2)
            }

            it("指定されたlimitとoffsetに従って結果を返す") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val limit = 2
                val result1 =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = limit,
                            offset = 0,
                        ),
                    )

                val result2 =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = limit,
                            offset = result1.nextOffset!!, // 次のページのoffsetを使用
                        ),
                    )

                result1.files.size shouldBe limit
                result2.files.size shouldBe limit
                result1.files.none { it in result2.files }.shouldBeTrue()
                result1.nextOffset shouldBe limit // 次のページのoffsetは2
                result2.nextOffset shouldBe limit * 2 // 次のページのoffsetは4
            }

            it("最後のページではnextOffsetがnullになる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val limit = 3
                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            limit = limit,
                            offset = 7, // 残り1ファイル（全8ファイル中）
                        ),
                    )

                result.files.size shouldBe 1 // 残りの1ファイル
                result.nextOffset.shouldBeNull() // これ以上ページがないのでnull
            }
        }

        describe("最大結果数制限") {
            it("maxResultsで指定された数以上の結果を返さない") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val maxResults = 3
                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            maxResults = maxResults,
                        ),
                    )

                result.totalCount shouldBe maxResults
                result.files.size shouldBe maxResults
                result.nextOffset.shouldBeNull() // maxResultsに達しているのでnull
            }
        }

        describe("パス表示オプション") {
            it("fullPathの設定に応じて適切なパス形式を返す") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val resultWithFullPath =
                    searcher.searchFiles(
                        FileSearchRequest(
                            fullPath = true,
                            limit = 1,
                        ),
                    )

                val resultWithFileName =
                    searcher.searchFiles(
                        FileSearchRequest(
                            fullPath = false,
                            limit = 1,
                        ),
                    )

                resultWithFullPath.files.first() shouldContain "/"
                resultWithFileName.files.first() shouldNotContain "/"
            }
        }

        describe("同じファイル名の異なるパスでの検索") {
            it("同じパターンで複数のファイルを検索できる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()

                // File1.ktとFile2.ktを検索（mainとtestの両方）
                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = "File[12]\\.kt$",
                            fullPath = true,
                        ),
                    )

                result.files.size shouldBe 4 // main:2 + test:2
                result.files.count { it.contains("src/main/kotlin") } shouldBe 2
                result.files.count { it.contains("src/test/kotlin") } shouldBe 2
                result.files.count { it.contains("File1.kt") } shouldBe 2
                result.files.count { it.contains("File2.kt") } shouldBe 2
            }

            it("パスを指定しない場合は一致するすべてのファイルが検索される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()

                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = "File[12]\\.kt$",
                            fullPath = true,
                        ),
                    )

                result.files.size shouldBe 4 // main:2 + test:2
                result.files.count { it.contains("src/main/kotlin") } shouldBe 2
                result.files.count { it.contains("src/test/kotlin") } shouldBe 2
                result.files.count { it.contains("File1.kt") } shouldBe 2
                result.files.count { it.contains("File2.kt") } shouldBe 2
            }

            it("同じファイル名で異なるパスのファイルが存在する場合、fullPath = falseでは同じファイル名が複数表示される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()

                // File1.ktを検索（mainとtestの両方に存在する）
                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = "File1\\.kt$",
                            fullPath = false, // ファイル名のみで検索
                        ),
                    )

                // 同じファイル名が複数回表示されることを確認（mainとtestの両方から）
                result.files.size shouldBe 2
                result.files.all { it == "File1.kt" }.shouldBeTrue()
            }
        }

        describe("エラーハンドリング") {
            it("無効な正規表現パターンでエラーを投げる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createFileSearcher()
                val result =
                    runCatching {
                        searcher.searchFiles(
                            FileSearchRequest(
                                pattern = "[", // 無効な正規表現
                            ),
                        )
                    }

                result.isFailure.shouldBeTrue()
                requireNotNull(result.exceptionOrNull())::class shouldBe IllegalArgumentException::class
            }
        }
    }
})
