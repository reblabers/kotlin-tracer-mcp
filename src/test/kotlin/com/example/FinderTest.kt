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
            describe("関数") {
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

            describe("クラス") {
                it("JavaClassとKtClassが相互に変換できる") {
                    // JavaClass -> KtClass -> JavaClass
                    val javaClass1 =
                        requireNotNull(finder.findJavaClass("org.example.threaddemo.services.OpService"))
                    val ktClass1 = requireNotNull(finder.findKtClass(javaClass1))
                    val javaClass2 = requireNotNull(finder.findJavaClass(ktClass1))
                    javaClass2.name shouldBe javaClass1.name

                    // KtClass -> JavaClass -> KtClass
                    val ktClass2 = requireNotNull(finder.findKtClass("org.example.threaddemo.services.OpService"))
                    val javaClass3 = requireNotNull(finder.findJavaClass(ktClass2))
                    val ktClass3 = requireNotNull(finder.findKtClass(javaClass3))
                    ktClass3.name shouldBe ktClass2.name
                }

                it("インナークラスでも相互に変換できる") {
                    // JavaClass -> KtClass -> JavaClass
                    val javaClass1 =
                        requireNotNull(finder.findJavaClass("org.example.threaddemo.services.ComplexService\$ComplexResult"))
                    val ktClass1 = requireNotNull(finder.findKtClass(javaClass1))
                    val javaClass2 = requireNotNull(finder.findJavaClass(ktClass1))
                    javaClass2.name shouldBe javaClass1.name

                    // KtClass -> JavaClass -> KtClass
                    val ktClass2 =
                        requireNotNull(finder.findKtClass("org.example.threaddemo.services.ComplexService\$ComplexResult"))
                    val javaClass3 = requireNotNull(finder.findJavaClass(ktClass2))
                    val ktClass3 = requireNotNull(finder.findKtClass(javaClass3))
                    ktClass3.name shouldBe ktClass2.name
                }
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

        describe("findKtClass") {
            describe("from qualifiedClassName") {
                it("存在するクラスの場合、対応するKtClassを返す") {
                    val ktClass =
                        requireNotNull(
                            finder.findKtClass(
                                "org.example.threaddemo.services.OpService",
                            ),
                        )
                    ktClass.name shouldBe "OpService"
                }

                it("インナークラスの場合、対応するKtClassを返す") {
                    val ktClass =
                        requireNotNull(
                            finder.findKtClass(
                                "org.example.threaddemo.services.ComplexService\$ComplexResult",
                            ),
                        )
                    ktClass.name shouldBe "ComplexResult"
                }

                it("存在しないクラスの場合、nullを返す") {
                    val ktClass =
                        finder.findKtClass(
                            "org.example.threaddemo.services.NonExistentClass",
                        )
                    ktClass shouldBe null
                }
            }
        }

        describe("findKtParameter") {
            it("存在するパラメータの場合、対応するKtParameterを返す") {
                val ktParameter =
                    requireNotNull(
                        finder.findKtParameter(
                            "org.example.threaddemo.services.ComplexService.helloRepository",
                        ),
                    )
                ktParameter.name shouldBe "helloRepository"
            }

            it("インナークラスのパラメータの場合、対応するKtParameterを返す") {
                val ktParameter =
                    requireNotNull(
                        finder.findKtParameter(
                            "org.example.threaddemo.services.ComplexService\$ComplexResult.answer",
                        ),
                    )
                ktParameter.name shouldBe "answer"
            }

            it("存在しないパラメータの場合、nullを返す") {
                val ktParameter =
                    finder.findKtParameter(
                        "org.example.threaddemo.services.ComplexService.nonExistentParameter",
                    )
                ktParameter shouldBe null
            }

            it("存在しないクラスのパラメータの場合、nullを返す") {
                val ktParameter =
                    finder.findKtParameter(
                        "org.example.threaddemo.services.NonExistentClass.parameter",
                    )
                ktParameter shouldBe null
            }
        }

        describe("findKtProperty") {
            it("存在するパラメータの場合、対応するKtParameterを返す") {
                val ktProperty =
                    requireNotNull(
                        finder.findKtProperty(
                            "org.example.threaddemo.repositories.WorldRepository.weight",
                        ),
                    )
                ktProperty.name shouldBe "weight"
            }

            it("存在しないパラメータの場合、nullを返す") {
                val ktProperty =
                    finder.findKtProperty(
                        "org.example.threaddemo.repositories.WorldRepository.nonExistentProperty",
                    )
                ktProperty shouldBe null
            }

            it("存在しないクラスのパラメータの場合、nullを返す") {
                val ktProperty =
                    finder.findKtParameter(
                        "org.example.threaddemo.services.NonExistentClass.parameter",
                    )
                ktProperty shouldBe null
            }
        }
    }
})
