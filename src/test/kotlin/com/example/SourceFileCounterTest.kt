package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SourceFileCounterTest : DescribeSpec({
    describe("SourceFileCounter") {
        describe("getTotalSourceFiles") {
            it("存在するすべてのソースファイルの数を返す") {
                val counter =
                    ToolFactory.forPath(TestProjects.SOURCE_COUNT_PROJECT.path)
                        .createSourceFileCounter()
                val result = counter.getTotalSourceFiles()

                result.size shouldBe 3 // File1.kt, sub/File2.kt, sub/File3.kt
            }

            it("存在しないパスの場合はエラーになる") {
                val result =
                    runCatching {
                        ToolFactory.forPath(TestProjects.SOURCE_COUNT_PROJECT.path.resolve("non-existent"))
                    }

                result.isFailure shouldBe true
            }

            it("複数のパスを指定してファイル数を取得できる") {
                val subPath = TestProjects.SOURCE_COUNT_PROJECT.path.resolve("sub")
                val counter =
                    ToolFactory.forPaths(listOf(subPath))
                        .createSourceFileCounter()
                val result = counter.getTotalSourceFiles()

                result.size shouldBe 2 // sub/File2.kt, sub/File3.kt
            }
        }
    }
})
