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
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.controllers.ComplexController" +
                            ".exec(org.example.threaddemo.controllers.ComplexRequest)",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.controllers.ComplexController.exec(ComplexRequest)", // from kt function
                        meta =
                            MethodMetaInfo(
                                name = "exec",
                                visibility = "public",
                                annotations = listOf("@PostMapping(\"/exec\")"),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("request", "ComplexRequest"),
                                    ),
                                returnType = "String",
                            ),
                    )
            }

            it("パラメータ付きのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.services.OpService.plus(int)",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.services.OpService.plus(int)",
                        meta =
                            MethodMetaInfo(
                                name = "plus",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("a", "int"),
                                    ),
                                returnType = "int",
                            ),
                    )
            }

            it("存在しないメソッドを指定した場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.controllers.ComplexController.nonExistentMethod()",
                            "org.example.threaddemo",
                        )
                    }

                exception.message shouldContain "Method not found"
            }

            it("不正なメソッド名フォーマットの場合はエラーを返す") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.controllers.ComplexController.exec",
                            "org.example.threaddemo",
                        )
                    }

                exception.message shouldContain "Invalid method name format"
            }

            it("複数定義のあるメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.converters.ComplexConverterKt.rootFun(int, int)",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.converters.rootFun(int,int)",
                        meta =
                            MethodMetaInfo(
                                name = "rootFun",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("a", "int"),
                                        ParameterInfo("b", "int"),
                                    ),
                                returnType = "int",
                            ),
                    )
            }

            it("privateメソッドの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.services.OpService.privateMethod()",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.services.OpService.privateMethod()",
                        meta =
                            MethodMetaInfo(
                                name = "privateMethod",
                                visibility = "private",
                                annotations = emptyList(),
                                modifiers = listOf("private"),
                                parameters = emptyList(),
                                returnType = "int",
                            ),
                    )
            }

            it("複数アノテーション付きのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.controllers.OpController.multiAnnotatedMethod()",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.controllers.OpController.multiAnnotatedMethod()",
                        meta =
                            MethodMetaInfo(
                                name = "multiAnnotatedMethod",
                                visibility = "public",
                                annotations = listOf("@GetMapping(\"/multi\")", "@ResponseBody"),
                                modifiers = emptyList(),
                                parameters = emptyList(),
                                returnType = "String",
                            ),
                    )
            }

            context("ジェネリック型を含むメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()

                val expected =
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.converters.ComplexConverter.convertGeneric(List<String>)",
                        meta =
                            MethodMetaInfo(
                                name = "convertGeneric",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("input", "List<String>"),
                                    ),
                                returnType = "List<Int>",
                            ),
                    )

                it("from JavaMethod") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.converters.ComplexConverter.convertGeneric(java.util.List)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }

                it("from kt function") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.converters.ComplexConverter.convertGeneric(List<String>)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }
            }

            it("suspendメソッドの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.services.OpService.suspendOperation()",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.services.OpService.suspendOperation()",
                        meta =
                            MethodMetaInfo(
                                name = "suspendOperation",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = listOf("suspend"),
                                parameters = emptyList(),
                                returnType = "String",
                            ),
                    )
            }

            context("インナークラスのメソッド情報を取得できる") {
                val expected =
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.services.OpService.InnerClass.multiply(int)",
                        meta =
                            MethodMetaInfo(
                                name = "multiply",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("a", "int"),
                                    ),
                                returnType = "int",
                            ),
                    )

                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()

                it("from JavaMethod") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.services.OpService\$InnerClass.multiply(int)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }

                it("from kt function") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.services.OpService.InnerClass.multiply(int)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }
            }

            context("コンパニオンオブジェクトのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val expected =
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.converters.ComplexConverter.Companion.default()",
                        meta =
                            MethodMetaInfo(
                                name = "default",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters = emptyList(),
                                returnType = "ComplexConverter",
                            ),
                    )

                it("from JavaMethod") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.converters.ComplexConverter\$Companion.default()",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }

                it("from kt function") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.converters.ComplexConverter.Companion.default()",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }
            }

            it("enumクラスのメソッド情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.converters.MultiplyScale.calculate(int)",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.converters.MultiplyScale.calculate(int)",
                        meta =
                            MethodMetaInfo(
                                name = "calculate",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("value", "int"),
                                    ),
                                returnType = "int",
                            ),
                    )
            }

            it("オーバーロードされたメソッドの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetail(
                        "org.example.threaddemo.converters.ComplexConverterKt.rootFun(int)",
                        "org.example.threaddemo",
                    )

                result.methodInfo shouldBe
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.converters.rootFun(int)",
                        meta =
                            MethodMetaInfo(
                                name = "rootFun",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("a", "int"),
                                    ),
                                returnType = "int",
                            ),
                    )
            }

            context("インターフェースのデフォルトメソッドの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val expected =
                    MethodInfo(
                        qualifiedName = "org.example.threaddemo.services.Op.divide(int)",
                        meta =
                            MethodMetaInfo(
                                name = "divide",
                                visibility = "public",
                                annotations = emptyList(),
                                modifiers = emptyList(),
                                parameters =
                                    listOf(
                                        ParameterInfo("a", "int"),
                                    ),
                                returnType = "int",
                            ),
                    )

                it("from JavaMethod inner class") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.services.OpService\$InnerClass.divide(int)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }

                it("from JavaMethod default") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.services.Op\$DefaultImpls.divide(org.example.threaddemo.services.Op, int)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }

                it("from kt function") {
                    val result =
                        analyzer.getMethodDetail(
                            "org.example.threaddemo.services.Op.divide(int)",
                            "org.example.threaddemo",
                        )
                    result.methodInfo shouldBe expected
                }
            }
        }

        describe("複数メソッドの情報取得") {
            it("複数の存在するメソッドの情報を取得できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "org.example.threaddemo.controllers.ComplexController.exec(org.example.threaddemo.controllers.ComplexRequest)",
                            "org.example.threaddemo.services.OpService.plus(int)",
                        ),
                        "org.example.threaddemo",
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames.shouldBeEmpty()

                result.results[0].methodInfo.qualifiedName shouldBe
                    "org.example.threaddemo.controllers.ComplexController.exec(ComplexRequest)"
                result.results[1].methodInfo.qualifiedName shouldBe "org.example.threaddemo.services.OpService.plus(int)"
            }

            it("存在しないメソッドがある場合は失敗リストに追加される") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "org.example.threaddemo.controllers.ComplexController.exec(org.example.threaddemo.controllers.ComplexRequest)",
                            "org.example.threaddemo.controllers.ComplexController.nonExistentMethod()",
                            "org.example.threaddemo.services.OpService.plus(int)",
                        ),
                        "org.example.threaddemo",
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames shouldContainExactly
                    listOf(
                        "org.example.threaddemo.controllers.ComplexController.nonExistentMethod()",
                    )

                result.results[0].methodInfo.qualifiedName shouldBe
                    "org.example.threaddemo.controllers.ComplexController.exec(ComplexRequest)"
                result.results[1].methodInfo.qualifiedName shouldBe "org.example.threaddemo.services.OpService.plus(int)"
            }

            it("不正なメソッド名フォーマットがある場合は失敗リストに追加される") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "org.example.threaddemo.controllers.ComplexController.exec(org.example.threaddemo.controllers.ComplexRequest)",
                            "org.example.threaddemo.controllers.ComplexController.invalidMethod",
                            "org.example.threaddemo.services.OpService.plus(int)",
                        ),
                        "org.example.threaddemo",
                    )

                result.results.size shouldBe 2
                result.failedQualifiedNames shouldContainExactly
                    listOf(
                        "org.example.threaddemo.controllers.ComplexController.invalidMethod",
                    )

                requireNotNull(result.results[0].methodInfo).qualifiedName shouldBe
                    "org.example.threaddemo.controllers.ComplexController.exec(ComplexRequest)"
                requireNotNull(result.results[1].methodInfo).qualifiedName shouldBe "org.example.threaddemo.services.OpService.plus(int)"
            }

            it("指定された複数のメソッドが存在しない場合は失敗リストに追加される") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodDetailAnalyzer()
                val result =
                    analyzer.getMethodDetails(
                        listOf(
                            "org.example.threaddemo.controllers.ComplexRequest.getName()",
                            "org.example.threaddemo.converters.ComplexConverter.<init>(int)",
                            "org.example.threaddemo.services.ComplexService\$ComplexResult.<init>(java.lang.String)",
                            "org.example.threaddemo.services.ComplexService\$ComplexResult.getAnswer()",
                        ),
                        "org.example.threaddemo",
                    )

                result.results.size shouldBe 4
                result.failedQualifiedNames.shouldBeEmpty()

                result.results[0].methodInfo.qualifiedName shouldBe "org.example.threaddemo.controllers.ComplexRequest.getName()"
                result.results[1].methodInfo.qualifiedName shouldBe "org.example.threaddemo.converters.ComplexConverter.<init>(int)"
                result.results[2].methodInfo.qualifiedName shouldBe
                    "org.example.threaddemo.services.ComplexService\$ComplexResult.<init>(java.lang.String)"
                result.results[3].methodInfo.qualifiedName shouldBe
                    "org.example.threaddemo.services.ComplexService\$ComplexResult.getAnswer()"
            }
        }
    }
})
