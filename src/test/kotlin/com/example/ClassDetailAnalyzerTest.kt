package com.example

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ClassDetailAnalyzerTest : DescribeSpec({
    describe("ClassDetailAnalyzer") {
        describe("単一クラスの情報取得") {
            it("通常のクラスの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MVC_PROJECT.path)
                        .createClassDetailAnalyzer()
                val result = analyzer.getClassDetail("org.example.threaddemo.controllers.OpController")

                result.qualifiedName shouldBe "org.example.threaddemo.controllers.OpController"
                result.sourceCode shouldContain "class OpController"
                result.sourceCode shouldContain "@RestController"
            }

            it("存在しないクラスを指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MVC_PROJECT.path)
                        .createClassDetailAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.getClassDetail("org.example.NonExistentClass")
                    }

                exception.message shouldContain "Class not found"
            }
        }

        describe("複数クラスの情報取得") {
            it("複数の存在するクラスの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassDetailAnalyzer()
                val result =
                    analyzer.getClassDetails(
                        listOf(
                            "com.example.File4",
                            "com.example.File5",
                        ),
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames.shouldBeEmpty()

                result.results[0].qualifiedName shouldBe "com.example.File4"
                result.results[1].qualifiedName shouldBe "com.example.File5"
            }

            it("存在しないクラスがある場合は失敗リストに追加される") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassDetailAnalyzer()
                val result =
                    analyzer.getClassDetails(
                        listOf(
                            "com.example.File4",
                            "com.example.NonExistentClass",
                            "com.example.File5",
                        ),
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames shouldContainExactly
                    listOf(
                        "com.example.NonExistentClass",
                    )

                result.results[0].qualifiedName shouldBe "com.example.File4"
                result.results[1].qualifiedName shouldBe "com.example.File5"
            }

            it("空のリストを指定すると空の結果が返される") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MVC_PROJECT.path)
                        .createClassDetailAnalyzer()
                val result = analyzer.getClassDetails(emptyList())

                result.results.shouldBeEmpty()
                result.failedQualifiedNames.shouldBeEmpty()
            }
        }
    }
})
