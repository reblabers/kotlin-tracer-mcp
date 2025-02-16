package com.example

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ClassMethodAnalyzerTest : DescribeSpec({
    describe("ClassMethodAnalyzer") {
        describe("基本的なメソッド情報の取得") {
            it("単純なクラスのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val result = analyzer.analyzeBySimpleName("File4")

                result.className shouldBe "File4"
                result.qualifiedName shouldBe "com.example.File4"
                result.methods shouldHaveSize 1
                result.methods.first() shouldBe
                    MethodInfo(
                        name = "method4",
                        qualifiedName = "com.example.File4.method4()",
                        visibility = "public",
                        annotations = listOf("@NotNull"),
                        modifiers = emptyList(),
                        parameters = emptyList(),
                        returnType = "Unit",
                    )
            }

            it("複数のメソッドを持つクラスの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val result = analyzer.analyzeBySimpleName("File3")

                result.className shouldBe "File3"
                result.qualifiedName shouldBe "com.example.File3"
                result.methods shouldHaveSize 2
                result.methods.map { it.name }.shouldContainExactlyInAnyOrder("method3", "method3v2")
            }
        }

        describe("可視性修飾子の解析") {
            it("各種可視性修飾子を持つメソッドを解析できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val result = analyzer.analyzeBySimpleName("File3")

                result.methods.map { it.visibility }.shouldContainExactlyInAnyOrder(
                    "public",
                    "public",
                )
            }
        }

        describe("パラメータの解析") {
            it("パラメータを持つメソッドを解析できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val result = analyzer.analyzeBySimpleName("File5")

                result.className shouldBe "File5"
                result.qualifiedName shouldBe "com.example.File5"
                result.methods shouldHaveSize 1
                result.methods.first() shouldBe
                    MethodInfo(
                        name = "method5",
                        qualifiedName = "com.example.File5.method5(int)",
                        visibility = "protected",
                        annotations = emptyList(),
                        modifiers = listOf("final", "protected"),
                        parameters = listOf(ParameterInfo("value", "int")),
                        returnType = "String",
                    )
            }
        }

        describe("クラス名の解決") {
            it("単純名で一意にクラスが特定できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val result = analyzer.analyzeBySimpleName("File1")

                result.className shouldBe "File1"
                result.qualifiedName shouldBe "com.example.File1"
            }

            it("単純名で複数のクラスが見つかる場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.analyzeBySimpleName("File2")
                    }

                exception.message shouldContain "Multiple classes found"
                exception.message shouldContain "com.example.File2"
                exception.message shouldContain "com.example.test.File2"
            }

            it("完全修飾名で一意にクラスが特定できる") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val result = analyzer.analyzeByFqName("com.example.File2")

                result.className shouldBe "File2"
                result.qualifiedName shouldBe "com.example.File2"
            }

            it("存在しないクラス名を指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPath(TestProjects.MANY_FILES_PROJECT.path)
                        .createClassMethodAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.analyzeBySimpleName("NonExistentClass")
                    }

                exception.message shouldContain "Class not found"
            }
        }
    }
})
