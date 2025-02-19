package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class MethodCallAnalyzerComplexTest : DescribeSpec({
    describe("MethodCallAnalyzer - 複雑なメソッド呼び出しの解析") {
        describe("複雑なサービスメソッドの解析") {
            it("内部クラスとコンパニオンオブジェクトを含むメソッド呼び出しを解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.services.ComplexService.exec(java.lang.String)",
                        "org.example.threaddemo",
                    )

                result.qualifiedName shouldBe "org.example.threaddemo.services.ComplexService.exec(java.lang.String)"
                result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.repositories.HelloRepository.count()",
                        "org.example.threaddemo.services.ComplexService\$InnerClass\$Companion.one()",
                        "org.example.threaddemo.services.ComplexService.privateMethod()",
                        "org.example.threaddemo.converters.ComplexConverter\$Companion.default()",
                        "org.example.threaddemo.converters.ComplexConverter.<init>(int)",
                        "org.example.threaddemo.converters.ComplexConverter.convert(int)",
                        "org.example.threaddemo.converters.MultiplyScale.calculate(int)",
                        "org.example.threaddemo.services.ComplexService.toResult(java.lang.String, java.util.List)",
                        "org.example.threaddemo.repositories.WorldRepository.count()",
                        "org.example.threaddemo.services.ComplexService\$ComplexResult.<init>(java.lang.String)",
                        "org.example.threaddemo.services.ComplexServiceKt.rootFun(int)",
                        "org.example.threaddemo.services.ComplexService\$InnerClass\$Companion.one(java.lang.String)",
                    )
            }

            it("privateメソッドの呼び出しを含むメソッドを解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.services.ComplexService.privateMethod()",
                        "org.example.threaddemo",
                    )

                result.qualifiedName shouldBe "org.example.threaddemo.services.ComplexService.privateMethod()"
                result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.repositories.WorldRepository.count()",
                        "org.example.threaddemo.services.ComplexService\$InnerClass\$Companion.one()",
                    )
            }
        }

        describe("コントローラーからサービスへの呼び出し解析") {
            it("コントローラーからサービスを経由した複雑な呼び出しを解析できる") {
                val analyzer =
                    ToolFactory.forPathWithJar(
                        TestProjects.MVC_PROJECT.path,
                        TestProjects.MVC_PROJECT.jarPath!!,
                    ).createMethodCallAnalyzer()

                val result =
                    analyzer.analyzeMethodCalls(
                        "org.example.threaddemo.controllers.ComplexController" +
                            ".exec(org.example.threaddemo.controllers.ComplexRequest)",
                        "org.example.threaddemo",
                        maxDepth = 3,
                    )

                result.qualifiedName shouldBe
                    "org.example.threaddemo.controllers.ComplexController" +
                    ".exec(org.example.threaddemo.controllers.ComplexRequest)"
                result.calledMethods shouldContainExactlyInAnyOrder
                    listOf(
                        "org.example.threaddemo.controllers.ComplexRequest.getName()",
                        "org.example.threaddemo.services.ComplexService.exec(java.lang.String)",
                        "org.example.threaddemo.repositories.HelloRepository.count()",
                        "org.example.threaddemo.services.ComplexService\$InnerClass\$Companion.one()",
                        "org.example.threaddemo.services.ComplexService.privateMethod()",
                        "org.example.threaddemo.converters.ComplexConverter\$Companion.default()",
                        "org.example.threaddemo.converters.ComplexConverter.<init>(int)",
                        "org.example.threaddemo.converters.ComplexConverter.convert(int)",
                        "org.example.threaddemo.converters.MultiplyScale.calculate(int)",
                        "org.example.threaddemo.services.ComplexService.toResult(java.lang.String, java.util.List)",
                        "org.example.threaddemo.services.ComplexService\$ComplexResult.<init>(java.lang.String)",
                        "org.example.threaddemo.repositories.WorldRepository.count()",
                        "org.example.threaddemo.services.ComplexService\$ComplexResult.getAnswer()",
                        "org.example.threaddemo.services.ComplexServiceKt.rootFun(int)",
                        "org.example.threaddemo.services.ComplexService\$InnerClass\$Companion.one(java.lang.String)",
                    )
            }
        }
    }
})
