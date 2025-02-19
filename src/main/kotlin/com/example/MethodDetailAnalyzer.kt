package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction

@Serializable
data class MethodDetailsResult(
    val results: List<MethodDetailResult>,
    val failedQualifiedNames: List<String>,
)

/**
 * メソッドの詳細情報を表すデータクラス
 *
 * @property methodInfo メソッドの基本情報（名前、パラメータ、修飾子など）
 * @property sourceCode メソッドの実装コード（シグネチャを含む）
 */
@Serializable
data class MethodDetailResult(
    // analyze_class_methodsの結果を含む
    val methodInfo: MethodInfo,
    // メソッドのソースコード（取得できたテキストをそのまま使用）
    val sourceCode: String,
) {
    companion object {
        /**
         * 関数からメソッド詳細情報を作成します。
         *
         * @param function 対象の関数
         * @return メソッド詳細情報
         */
        fun from(function: KtFunction): MethodDetailResult {
            return MethodDetailResult(
                methodInfo = MethodInfo.from(function),
                sourceCode = function.text,
            )
        }
    }
}

/**
 * メソッドの詳細情報を解析するクラス
 *
 * @property resources ソースコードリソースへのアクセスを提供するインスタンス
 */
class MethodDetailAnalyzer(
    private val resources: Resources,
) {
    /**
     * 複数のメソッドの詳細情報を取得します。
     *
     * @param qualifiedMethodNames メソッドの完全修飾名のリスト（例：["org.example.MyClass.myMethod(int,long)", "org.example.MyClass.otherMethod()"]）
     * @return メソッドの詳細情報と失敗したメソッド名のリスト
     */
    fun getMethodDetails(
        qualifiedMethodNames: List<String>,
        methodSearchScope: String,
    ): MethodDetailsResult {
        val successResults = mutableListOf<MethodDetailResult>()
        val failedQualifiedNames = mutableListOf<String>()

        qualifiedMethodNames.forEach { qualifiedMethodName ->
            try {
                val result = getMethodDetail(qualifiedMethodName, methodSearchScope)
                successResults.add(result)
            } catch (_: IllegalArgumentException) {
                failedQualifiedNames.add(qualifiedMethodName)
            }
        }

        return MethodDetailsResult(successResults, failedQualifiedNames)
    }

    /**
     * 単一のメソッドの詳細情報を取得します。
     *
     * @param qualifiedMethodName メソッドの完全修飾名（例：org.example.MyClass.myMethod(int,long)）
     * @return メソッドの詳細情報
     * @throws IllegalArgumentException メソッドが見つからない場合
     */
    fun getMethodDetail(
        qualifiedMethodName: String,
        methodSearchScope: String,
    ): MethodDetailResult {
        require(qualifiedMethodName.contains("(") && qualifiedMethodName.endsWith(")")) {
            "Invalid method name format. Expected: package.Class.method(param1,param2) or package.function(param1,param2)"
        }

        val targetFqName = FqName(qualifiedMethodName)
        val methods =
            resources.allSources().files
                .filter { targetFqName.isOrInsideOf(it.packageFqName) }
                .flatMap { it.readonly.functionList() }

        // find from kt function
        val targetMethod = methods.find { toQualifiedName(it) == qualifiedMethodName }
        if (targetMethod != null) {
            return MethodDetailResult.from(targetMethod)
        }

        return findFromJavaClasses(qualifiedMethodName, methodSearchScope, methods)
    }

    private fun findFromJavaClasses(
        qualifiedMethodName: String,
        methodSearchScope: String,
        methods: List<KtFunction>,
    ): MethodDetailResult {
        val targetMethod =
            resources.classesInPackage(methodSearchScope)
                .flatMap { it.methods }
                .filter {
                    println(it)
                    true
                }
                .find { it.fullName == qualifiedMethodName }
                ?: throw IllegalArgumentException("Method not found: $qualifiedMethodName")

        val functions =
            methods
                .filter { it.name == targetMethod.name }
                .toList()

        if (functions.size == 1) {
            return MethodDetailResult.from(functions.single())
        }

        if (functions.isNotEmpty()) {
            val targetParameterTypes =
                targetMethod.parameterTypes
                    .map { it.name }.map { it.substringAfterLast(".") }.map { it.lowercase() }
            for (function in functions) {
                val parameterTypes =
                    function.valueParameters
                        .map { toTypeName(it) }.map { it.substringAfterLast(".") }
                        .map { it.lowercase() }
                if (parameterTypes == targetParameterTypes) {
                    return MethodDetailResult.from(function)
                }
            }
        }

        throw IllegalArgumentException("Method not found in source files: $qualifiedMethodName")
    }
}
