package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FinderTest : DescribeSpec({
    describe("Finder") {
        val resources =
            EnvironmentProvider.create(
                listOf(TestProjects.MVC_PROJECT.path),
                listOf(TestProjects.MVC_PROJECT.jarPath!!),
            )
        val finder = resources.createFinder("org.example.threaddemo")

        describe("相互変換") {
            it("JavaMethodとKtFunctionが相互に変換できる") {
                // JavaMethod -> KtFunction -> JavaMethod
                val javaMethod1 =
                    requireNotNull(
                        finder.findJavaMethod(
                            "org.example.threaddemo.services.OpService.plus(int)",
                        ),
                    )

                val ktFunction1 = requireNotNull(finder.findKtFunction(javaMethod1))

                val javaMethod2 = requireNotNull(finder.findJavaMethod(ktFunction1))
                javaMethod2.fullName shouldBe javaMethod1.fullName

                // KtFunction -> JavaMethod -> KtFunction
                val ktFunction2 =
                    requireNotNull(
                        finder.findKtFunction(
                            "org.example.threaddemo.services.OpService.plus(int)",
                        ),
                    )

                val javaMethod3 = requireNotNull(finder.findJavaMethod(ktFunction2))

                val ktFunction3 = requireNotNull(finder.findKtFunction(javaMethod3))
                ktFunction3.name shouldBe ktFunction2.name
                ktFunction3.valueParameters.size shouldBe ktFunction2.valueParameters.size
            }

            it("オーバーロードされたメソッドでも相互に変換できる") {
                // JavaMethod -> KtFunction -> JavaMethod
                val javaMethod1 =
                    requireNotNull(
                        finder.findJavaMethod(
                            "org.example.threaddemo.converters.ComplexConverterKt.rootFun(int, int)",
                        ),
                    )

                val ktFunction1 = requireNotNull(finder.findKtFunction(javaMethod1))

                val javaMethod2 = requireNotNull(finder.findJavaMethod(ktFunction1))
                javaMethod2.fullName shouldBe javaMethod1.fullName

                // KtFunction -> JavaMethod -> KtFunction
                val ktFunction2 =
                    requireNotNull(
                        finder.findKtFunction(
                            "org.example.threaddemo.converters.rootFun(int)",
                        ),
                    )

                val javaMethod3 = requireNotNull(finder.findJavaMethod(ktFunction2))

                val ktFunction3 = requireNotNull(finder.findKtFunction(javaMethod3))
                ktFunction3.name shouldBe ktFunction2.name
                ktFunction3.valueParameters.size shouldBe ktFunction2.valueParameters.size
            }
        }

        describe("findJavaMethod") {
            describe("from qualifiedMethodName") {
                it("メソッド名と引数の型が一致する場合、対応するKtFunctionを返す") {
                    val javaMethod =
                        requireNotNull(
                            finder.findJavaMethod(
                                "org.example.threaddemo.services.OpService.plus(int)",
                            ),
                        )

                    val ktFunction = requireNotNull(finder.findKtFunction(javaMethod))
                    ktFunction.name shouldBe "plus"
                }

                it("オーバーロードされたメソッドの場合、引数の型が一致するKtFunctionを返す") {
                    val javaMethod =
                        requireNotNull(
                            finder.findJavaMethod(
                                "org.example.threaddemo.converters.ComplexConverterKt.rootFun(int, int)",
                            ),
                        )

                    val ktFunction = requireNotNull(finder.findKtFunction(javaMethod))
                    ktFunction.name shouldBe "rootFun"
                    ktFunction.valueParameters.size shouldBe 2
                }

                it("存在しないメソッドの場合、nullを返す") {
                    val javaMethod =
                        finder.findJavaMethod(
                            "org.example.threaddemo.services.OpService.nonExistentMethod()",
                        )
                    javaMethod shouldBe null
                }

                it("パラメータの型が異なる場合、nullを返す") {
                    val javaMethod =
                        finder.findJavaMethod(
                            "org.example.threaddemo.services.OpService.plus(string)",
                        )
                    javaMethod shouldBe null
                }
            }
        }

        describe("findKtFunction") {
            describe("from qualifiedMethodName") {
                it("メソッド名と引数の型が一致する場合、対応するJavaMethodを返す") {
                    val ktFunction =
                        requireNotNull(
                            finder.findKtFunction(
                                "org.example.threaddemo.services.OpService.plus(int)",
                            ),
                        )

                    val javaMethod = requireNotNull(finder.findJavaMethod(ktFunction))
                    javaMethod.name shouldBe "plus"
                }

                it("オーバーロードされたメソッドの場合、引数の型が一致するJavaMethodを返す") {
                    val ktFunction =
                        requireNotNull(
                            finder.findKtFunction(
                                "org.example.threaddemo.converters.rootFun(int)",
                            ),
                        )

                    val javaMethod = requireNotNull(finder.findJavaMethod(ktFunction))
                    javaMethod.name shouldBe "rootFun"
                    javaMethod.parameterTypes.size shouldBe 1
                }

                it("存在しないメソッドの場合、nullを返す") {
                    val ktFunction =
                        finder.findKtFunction(
                            "org.example.threaddemo.services.OpService.nonExistentMethod()",
                        )
                    ktFunction shouldBe null
                }

                it("パラメータの型が異なる場合、nullを返す") {
                    val ktFunction =
                        finder.findKtFunction(
                            "org.example.threaddemo.services.OpService.plus(string)",
                        )
                    ktFunction shouldBe null
                }
            }
        }
    }
})
