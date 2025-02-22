package com.example

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class MethodCallAnalyzerTest : DescribeSpec({
    describe("MethodCallAnalyzer") {
        describe("基本的なメソッド呼び出しの解析") {
            it("単純なメソッド呼び出しを解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.controllers.HelloController.processors()",
                        "org.example.threaddemo",
                    )

                result.qualifiedName shouldBe "org.example.threaddemo.controllers.HelloController.processors()"
                result.calledMethods shouldBe emptyList()
            }

            it("存在しないメソッドを指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.analyzeMethodCalls(
                            "org.example.threaddemo.controllers.HelloController.nonExistentMethod()",
                            "org.example.threaddemo",
                        )
                    }

                exception.message shouldBe "Method not found"
            }
        }

        describe("パッケージ判定機能") {
            it("プロジェクト内のパッケージのメソッド呼び出しのみを解析する") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.services.OpService.multiply()",
                        "org.example.threaddemo",
                    )

                result.qualifiedName shouldBe "org.example.threaddemo.services.OpService.multiply()"
                result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.repositories.HelloRepository.count()",
                        "org.example.threaddemo.services.OpService.privateMethod()",
                        "org.example.threaddemo.repositories.WorldRepository.count()",
                        "org.example.threaddemo.repositories.WorldRepository.getWeight()",
                    )
            }
        }

        describe("メソッド呼び出しの再帰的な解析") {
            it("深さ制限を指定して解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()
                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.services.HelloService.heavyWorkWithMemory(long)",
                        "org.example.threaddemo",
                        maxDepth = 2,
                    )

                result.qualifiedName shouldBe "org.example.threaddemo.services.HelloService.heavyWorkWithMemory(long)"
                result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.services.HelloService.lcgWithMemory(long, long)",
                    )
            }

            it("深さに応じて3層構造のメソッド呼び出しを解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                // 深さ1: Controller -> Service
                val depth1Result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.controllers.OpController.multiply()",
                        "org.example.threaddemo",
                        maxDepth = 1,
                    )
                depth1Result.qualifiedName shouldBe "org.example.threaddemo.controllers.OpController.multiply()"
                depth1Result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.services.OpService.multiply()",
                    )

                // 深さ2: Controller -> Service -> Repository
                val depth2Result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.controllers.OpController.multiply()",
                        "org.example.threaddemo",
                        maxDepth = 2,
                    )
                depth2Result.qualifiedName shouldBe "org.example.threaddemo.controllers.OpController.multiply()"
                depth2Result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.services.OpService.multiply()",
                        "org.example.threaddemo.repositories.HelloRepository.count()",
                        "org.example.threaddemo.services.OpService.privateMethod()",
                        "org.example.threaddemo.repositories.WorldRepository.count()",
                    )
            }
        }

        describe("修飾されたメソッド呼び出しの解析") {
            it("アノテーション付きのメソッド呼び出しを解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()
                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.services.OpService.plus(int)",
                        "org.example.threaddemo",
                    )

                result.qualifiedName shouldBe "org.example.threaddemo.services.OpService.plus(int)"
                result.calledMethods shouldBe emptyList()
            }
        }

        describe("メソッド名のバリデーション") {
            it("パラメータリストのない不正なメソッド名を指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.analyzeMethodCalls(
                            "org.example.threaddemo.services.OpService.plus",
                            "org.example.threaddemo",
                        )
                    }

                exception.message shouldBe "Qualified name must contain a parameter list"
            }

            it("パラメータリストが閉じていない不正なメソッド名を指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.analyzeMethodCalls(
                            "org.example.threaddemo.services.OpService.plus(",
                            "org.example.threaddemo",
                        )
                    }

                exception.message shouldBe "Qualified name must contain a parameter list"
            }
        }
    }
})
