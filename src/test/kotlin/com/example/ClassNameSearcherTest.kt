package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ClassNameSearcherTest : DescribeSpec({
    describe("ClassNameSearcher") {
        describe("基本的な検索機能") {
            it("デフォルト設定で全クラスを検索できる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val result = searcher.searchClassNames(ClassNameSearchRequest())

                result.qualifiedClassNames.shouldNotBeEmpty()
                // MANY_FILES_PROJECTには8つのファイルがあり、各ファイルに1つのクラスがある
                result.qualifiedClassNames.size shouldBe 8
                result.totalCount shouldBe 8
                result.nextOffset shouldBe null // 全件取得できているので次のページはない
            }

            it("ページネーションが正しく動作する") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = 3,
                            offset = 0,
                        ),
                    )

                result.qualifiedClassNames.size shouldBe 3
                result.totalCount shouldBe 8
                result.nextOffset shouldBe 3

                // 次のページを取得
                val nextResult =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = 3,
                            offset = result.nextOffset!!,
                        ),
                    )

                nextResult.qualifiedClassNames.size shouldBe 3
                nextResult.totalCount shouldBe 8
                nextResult.nextOffset shouldBe 6
            }

            it("検索結果が一貫した順序で返される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()

                // 同じ検索を2回実行
                val result1 =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = 5,
                        ),
                    )

                val result2 =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = 5,
                        ),
                    )

                // 結果が同じ順序であることを確認
                result1.qualifiedClassNames shouldBe result2.qualifiedClassNames
            }

            it("ページングの結果が一貫した順序で返される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val limit = 2

                // 1ページ目を取得
                val result1 =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = limit,
                            offset = 0,
                        ),
                    )

                // 2ページ目を取得
                val result2 =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = limit,
                            offset = result1.nextOffset!!,
                        ),
                    )

                // 全件を一度に取得
                val fullResult =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(),
                    )

                // ページングの結果が全件取得の結果と一致することを確認
                (result1.qualifiedClassNames + result2.qualifiedClassNames) shouldBe fullResult.qualifiedClassNames.take(limit * 2)
            }

            it("最後のページではnextOffsetがnullになる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val limit = 3
                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            limit = limit,
                            offset = 7, // 残り1クラス
                        ),
                    )

                result.qualifiedClassNames.size shouldBe 1 // 残りの1クラス
                result.nextOffset shouldBe null // これ以上ページがないのでnull
            }
        }

        describe("正規表現検索") {
            it("パターンに一致するクラス名のみを返す") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            // File2とFile2Testにマッチ
                            pattern = ".*File2(Test)?$",
                            fullPath = true,
                        ),
                    )

                result.qualifiedClassNames.size shouldBe 3 // main:1 (File2) + test:2 (File2, File2Test)
                result.totalCount shouldBe 3
                result.nextOffset shouldBe null
                result.qualifiedClassNames.all { it.endsWith("File2") || it.endsWith("File2Test") }.shouldBeTrue()
            }

            it("完全修飾名に対してパターンマッチングを行う") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()

                // パッケージ名を含むパターンでマッチング
                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            pattern = "com\\.example\\.File1$",
                            fullPath = true, // 完全修飾名を取得するために必要
                        ),
                    )
                result.qualifiedClassNames.size shouldBe 1 // main:1
                result.totalCount shouldBe 1
                result.nextOffset shouldBe null
                result.qualifiedClassNames.first() shouldBe "com.example.File1"
            }

            describe("パターンマッチングと表示形式の組み合わせ") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()

                it("完全修飾名での検索＆完全修飾名での表示") {
                    val result =
                        searcher.searchClassNames(
                            ClassNameSearchRequest(
                                pattern = "com\\.example\\.File1$",
                                fullPath = true,
                            ),
                        )
                    result.qualifiedClassNames.size shouldBe 1
                    result.qualifiedClassNames.first() shouldContain "com.example"
                    result.qualifiedClassNames.first() shouldContain "File1"
                }

                it("完全修飾名での検索＆クラス名のみ表示") {
                    val result =
                        searcher.searchClassNames(
                            ClassNameSearchRequest(
                                pattern = "com\\.example\\.File1$",
                                fullPath = false,
                            ),
                        )
                    result.qualifiedClassNames.size shouldBe 1
                    result.qualifiedClassNames.first() shouldBe "File1"
                }

                it("クラス名のみでの検索＆クラス名のみ表示") {
                    val result =
                        searcher.searchClassNames(
                            ClassNameSearchRequest(
                                pattern = "File1$",
                                fullPath = false,
                            ),
                        )
                    result.qualifiedClassNames.size shouldBe 1 // mainのみ
                    result.qualifiedClassNames.first() shouldBe "File1"
                }

                it("クラス名のみでの検索＆完全修飾名での表示") {
                    val result =
                        searcher.searchClassNames(
                            ClassNameSearchRequest(
                                pattern = "File1$",
                                fullPath = true,
                            ),
                        )
                    result.qualifiedClassNames.size shouldBe 1 // mainのみ
                    result.qualifiedClassNames.first() shouldContain "com.example"
                    result.qualifiedClassNames.first() shouldContain "File1"
                }

                it("パッケージの一部を含むパターンでの検索") {
                    val result =
                        searcher.searchClassNames(
                            ClassNameSearchRequest(
                                pattern = "com\\.example\\.File1$",
                                fullPath = false,
                            ),
                        )
                    result.qualifiedClassNames.size shouldBe 1
                    result.qualifiedClassNames.first() shouldBe "File1"
                }
            }

            it("存在しないクラス名パターンでは結果が空になる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            pattern = "NonExistentClass",
                        ),
                    )
                result.qualifiedClassNames.shouldBeEmpty()
                result.totalCount shouldBe 0
                result.nextOffset shouldBe null
            }
        }

        describe("maxResults制限") {
            it("maxResultsで結果が制限される") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            maxResults = 3,
                        ),
                    )

                result.qualifiedClassNames.size shouldBe 3
                result.totalCount shouldBe 3
                result.nextOffset shouldBe null
            }
        }

        describe("エラーハンドリング") {
            it("無効な正規表現パターンでエラーを投げる") {
                val searcher =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassNameSearcher()
                val result =
                    runCatching {
                        searcher.searchClassNames(
                            ClassNameSearchRequest(
                                // 無効な正規表現
                                pattern = "[",
                            ),
                        )
                    }

                result.isFailure.shouldBeTrue()
                requireNotNull(result.exceptionOrNull())::class shouldBe IllegalArgumentException::class
            }
        }
    }
})
