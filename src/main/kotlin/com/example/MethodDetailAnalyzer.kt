package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

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

        fun from(
            qualifiedName: String,
            parameter: KtParameter,
        ): MethodDetailResult {
            return MethodDetailResult(
                methodInfo = MethodInfo.from(qualifiedName),
                sourceCode = parameter.text,
            )
        }

        fun from(
            qualifiedName: String,
            property: KtProperty,
        ): MethodDetailResult {
            return MethodDetailResult(
                methodInfo = MethodInfo.from(qualifiedName),
                sourceCode = property.text,
            )
        }

        fun from(
            qualifiedName: String,
            constructor: KtConstructor<*>,
        ): MethodDetailResult {
            return MethodDetailResult(
                methodInfo = MethodInfo.from(qualifiedName),
                sourceCode = constructor.text,
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
        val finder = Finder(resources, methodSearchScope)

        val successResults = mutableListOf<MethodDetailResult>()
        val failedQualifiedNames = mutableListOf<String>()

        qualifiedMethodNames.forEach { qualifiedMethodName ->
            try {
                val result = getMethodDetail(qualifiedMethodName, methodSearchScope, finder)
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
        val finder = Finder(resources, methodSearchScope)
        return getMethodDetail(qualifiedMethodName, methodSearchScope, finder)
    }

    fun getMethodDetail(
        qualifiedMethodName: String,
        methodSearchScope: String,
        finder: Finder,
    ): MethodDetailResult {
        require(qualifiedMethodName.contains("(") && qualifiedMethodName.endsWith(")")) {
            "Invalid method name format. Expected: package.Class.method(param1,param2) or package.function(param1,param2)"
        }

        val targetFqName = FqName(qualifiedMethodName)
        if (!targetFqName.isOrInsideOf(FqName(methodSearchScope))) {
            throw IllegalArgumentException("Method not found in search scope")
        }

        // JavaMethodとして検索
        finder.findKtFunction(qualifiedMethodName)
            ?.let { return MethodDetailResult.from(it) }

        // KtFunctionとして見つからない場合、JavaMethodとして検索
        finder.findJavaMethod(qualifiedMethodName)
            ?.let { finder.findKtFunction(it) }
            ?.let { return MethodDetailResult.from(it) }

        // 明示的なメソッドが存在しないgetter/setterを探索
        Regex("^(.+)\\.(get|set)(.*)\\(\\)$").find(qualifiedMethodName)
            ?.let {
                val string = it.groupValues[1]
                val propertyName = it.groupValues[3].replaceFirstChar { it.lowercase() }
                "$string.$propertyName"
            }
            ?.let {
                finder.findKtParameter(it)
                    ?.let { return MethodDetailResult.from(qualifiedMethodName, it) }

                finder.findKtProperty(it)
                    ?.let { return MethodDetailResult.from(qualifiedMethodName, it) }
            }

        // <init>が含まれるか
        if (qualifiedMethodName.contains("<init>")) {
            finder.findKtConstructor(qualifiedMethodName)
                ?.let {
                    return MethodDetailResult.from(qualifiedMethodName, it)
                }
        }

        throw IllegalArgumentException("Method not found")
    }
}
