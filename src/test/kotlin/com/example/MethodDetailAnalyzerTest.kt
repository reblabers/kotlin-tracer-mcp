package com.example

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MethodDetailAnalyzerTest : DescribeSpec({
    describe("MethodDetailAnalyzer") {
        describe("単一メソッドの情報取得") {
            it("アノテーション付きのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val result = analyzer.getMethodDetail("com.example.File4.method4()")

                result.methodInfo shouldBe
                    MethodInfo(
                        name = "method4",
                        qualifiedName = "com.example.File4.method4()",
                        visibility = "public",
                        annotations = listOf("@NotNull"),
                        modifiers = emptyList(),
                        parameters = emptyList(),
                        returnType = "Unit",
                    )
                result.sourceCode shouldBe
                    """
@NotNull
    fun method4() {
        println("File4 method4")
    }
""".trim()
            }

            it("関数名が重複したのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(TestProjects.MVC_PROJECT.path, TestProjects.MVC_PROJECT.jarPath!!)
                        .createMethodDetailAnalyzer()
                val result = analyzer.getMethodDetail("org.example.threaddemo.services.OpService.multiply()")

                result.methodInfo shouldBe
                    MethodInfo(
                        name = "multiply",
                        qualifiedName = "org.example.threaddemo.services.OpService.multiply()",
                        visibility = "public",
                        annotations = emptyList(),
                        modifiers = emptyList(),
                        parameters = emptyList(),
                        returnType = "int",
                    )
            }

            it("パラメータ付きのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val result = analyzer.getMethodDetail("com.example.File5.method5(int)")

                result.methodInfo shouldBe
                    MethodInfo(
                        name = "method5",
                        qualifiedName = "com.example.File5.method5(int)",
                        visibility = "protected",
                        annotations = emptyList(),
                        modifiers = listOf("final", "protected"),
                        parameters = listOf(ParameterInfo("value", "int")),
                        returnType = "String",
                    )
                result.sourceCode shouldBe
                    """
protected final fun method5(value: Int): String {
        return value.toString()
    }
""".trim()
            }

            it("存在しないメソッドを指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.getMethodDetail("com.example.File4.nonExistentMethod()")
                    }

                exception.message shouldContain "Method not found"
            }

            it("不正なメソッド名フォーマットの場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.getMethodDetail("com.example.File4.method4") // カッコがない
                    }

                exception.message shouldContain "Invalid method name format"
            }
        }

        describe("複数メソッドの情報取得") {
            it("複数の存在するメソッドの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "com.example.File4.method4()",
                            "com.example.File5.method5(int)",
                        ),
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames.shouldBeEmpty()

                result.results[0].methodInfo.name shouldBe "method4"
                result.results[1].methodInfo.name shouldBe "method5"
            }

            it("存在しないメソッドがある場合は失敗リストに追加される") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "com.example.File4.method4()",
                            "com.example.File4.nonExistentMethod()",
                            "com.example.File5.method5(int)",
                        ),
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames shouldContainExactly
                    listOf(
                        "com.example.File4.nonExistentMethod()",
                    )

                result.results[0].methodInfo.name shouldBe "method4"
                result.results[1].methodInfo.name shouldBe "method5"
            }

            it("不正なメソッド名フォーマットがある場合は失敗リストに追加される") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "com.example.File4.method4()",
                            "com.example.File4.invalidMethod", // カッコがない
                            "com.example.File5.method5(int)",
                        ),
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames shouldContainExactly
                    listOf(
                        "com.example.File4.invalidMethod",
                    )

                result.results[0].methodInfo.name shouldBe "method4"
                result.results[1].methodInfo.name shouldBe "method5"
            }
        }
    }
})
