package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtClass
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
    fun getMethodDetails(qualifiedMethodNames: List<String>): MethodDetailsResult {
        val successResults = mutableListOf<MethodDetailResult>()
        val failedQualifiedNames = mutableListOf<String>()

        qualifiedMethodNames.forEach { qualifiedMethodName ->
            try {
                val result = getMethodDetail(qualifiedMethodName)
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
    fun getMethodDetail(qualifiedMethodName: String): MethodDetailResult {
        // メソッド名とパラメータ情報を分離
        val (methodNameParts, parameterList) = parseQualifiedMethodName(qualifiedMethodName)

        // プロジェクト内のすべてのソースファイルを取得
        val sourceFiles = resources.allSources()

        // プロジェクト内のすべてのソースファイルからメソッドを検索
        for (sourceFile in sourceFiles.files) {
            val ktFile = sourceFile.readonly

            // クラスメソッドを検索
            ktFile.classOrInterfaceList().forEach { ktClass ->
                val method = findMethodInClass(ktClass, methodNameParts, parameterList)
                if (method != null) {
                    return MethodDetailResult.from(method)
                }
            }

            // トップレベル関数を検索
            ktFile.functionList().forEach { function ->
                val method = findMethodInTopLevel(function, methodNameParts, parameterList)
                if (method != null) {
                    return MethodDetailResult.from(method)
                }
            }
        }

        throw IllegalArgumentException("Method not found")
    }

    /**
     * 完全修飾メソッド名を解析し、メソッド名のパーツとパラメータリストに分解します。
     *
     * @param qualifiedMethodName 完全修飾メソッド名（例：org.example.MyClass.myMethod(int,long)）
     * @return メソッド名のパーツとパラメータリストのペア
     * @throws IllegalArgumentException 不正なメソッド名フォーマットの場合
     */
    private fun parseQualifiedMethodName(qualifiedMethodName: String): Pair<List<String>, List<String>> {
        require(qualifiedMethodName.contains("(") && qualifiedMethodName.endsWith(")")) {
            "Invalid method name format. Expected: package.Class.method(param1,param2) or package.function(param1,param2)"
        }

        val methodNamePart = qualifiedMethodName.substringBefore("(")
        val parameterPart =
            qualifiedMethodName.substring(
                qualifiedMethodName.indexOf("(") + 1,
                qualifiedMethodName.lastIndexOf(")"),
            )

        val methodNameParts = methodNamePart.split(".")
        val parameterList =
            if (parameterPart.isBlank()) {
                emptyList()
            } else {
                parameterPart.split(",").map { it.trim() }
            }

        return Pair(methodNameParts, parameterList)
    }

    /**
     * クラス内から指定されたメソッドを検索します。
     *
     * @param ktClass 検索対象のクラス
     * @param methodNameParts メソッド名を含む完全修飾名のパーツ
     * @param parameterList パラメータ型のリスト
     * @return 見つかったメソッド、見つからない場合はnull
     */
    private fun findMethodInClass(
        ktClass: KtClass,
        methodNameParts: List<String>,
        parameterList: List<String>,
    ): KtFunction? {
        val classQualifiedName = ktClass.fqName?.asString() ?: return null
        val expectedClassPath = methodNameParts.dropLast(1).joinToString(".")

        if (classQualifiedName != expectedClassPath) {
            return null
        }

        return ktClass.declarations
            .filterIsInstance<KtFunction>()
            .find { function ->
                isMatchingFunction(function, methodNameParts, parameterList)
            }
    }

    private fun findMethodInTopLevel(
        function: KtFunction,
        methodNameParts: List<String>,
        parameterList: List<String>,
    ): KtFunction? {
        val classQualifiedName = function.fqName?.asString() ?: return null
        val expectedClassPath = methodNameParts.dropLast(1).joinToString(".")

        if (classQualifiedName != expectedClassPath) {
            return null
        }

        if (!isMatchingFunction(function, methodNameParts, parameterList)) {
            return null
        }

        return function
    }

    /**
     * 関数が指定された条件に一致するかチェックします。
     *
     * @param function チェック対象の関数
     * @param methodNameParts メソッド名を含む完全修飾名のパーツ
     * @param parameterList パラメータ型のリスト
     * @return 条件に一致する場合はtrue
     */
    private fun isMatchingFunction(
        function: KtFunction,
        methodNameParts: List<String>,
        parameterList: List<String>,
    ): Boolean {
        function.fqName
        if (function.name != methodNameParts.last()) {
            return false
        }

        val functionParameters = function.valueParameters.map { ParameterInfo.from(it).type }
        if (functionParameters.size != parameterList.size) {
            return false
        }

        return functionParameters.zip(parameterList).all { (actual, expected) ->
            actual == expected
        }
    }
}
