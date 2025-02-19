package com.example

import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.name.FqName

/**
 * メソッド呼び出し解析の結果を表すデータクラス
 *
 * @property qualifiedName メソッドの完全修飾名
 * @property calledMethods 呼び出されているメソッドの完全修飾名リスト
 */
@Serializable
data class MethodCallResult(
    val qualifiedName: String,
    val calledMethods: List<String>,
)

/**
 * メソッド呼び出し関係を解析するクラス
 * ArchUnitを使用してJavaバイトコードレベルでメソッド呼び出しを解析します
 *
 * @property resources ソースコードリソースへのアクセスを提供するインスタンス
 * @property targetPackages 解析対象のパッケージ一覧
 */
class MethodCallAnalyzer(
    private val resources: Resources,
) {
    // 解析対象のパッケージ一覧
    private val targetPackages: Set<FqName> by lazy {
        resources.allSources().packages()
    }

    // 内部実装用のsealed class
    private sealed class CallInfo {
        abstract val targetName: String

        data class RegularMethodCall(
            override val targetName: String,
            val ownerClass: String,
            val methodName: String,
            val parameterTypes: List<String>,
        ) : CallInfo()

        data class ConstructorCall(
            override val targetName: String,
            val ownerClass: String,
            val parameterTypes: List<String>,
        ) : CallInfo()

        data class MethodReference(
            override val targetName: String,
            val ownerClass: String,
            val methodName: String,
        ) : CallInfo()

        data class Annotation(
            override val targetName: String,
            val ownerClass: String,
        ) : CallInfo()
    }

    /**
     * 指定されたメソッドの呼び出し関係を解析する
     *
     * @param qualifiedName 完全修飾メソッド名 (例: "org.example.MyClass.myMethod(int)")
     * @param methodSearchScope メソッドを検索するパッケージスコープ (例: "org.example")
     * @param maxDepth 解析する最大深さ
     * @return メソッド呼び出し解析結果
     * @throws IllegalArgumentException メソッドが見つからない場合
     */
    fun analyzeMethodCalls(
        qualifiedName: String,
        methodSearchScope: String,
        maxDepth: Int = 5,
    ): MethodCallResult {
        require(qualifiedName.contains("(") && qualifiedName.endsWith(")")) { "Qualified name must contain a parameter list" }

        val targetMethod =
            resources.classesInPackage(methodSearchScope)
                .flatMap { it.methods }
                .find { it.fullName == qualifiedName }
                ?: throw IllegalArgumentException("Method not found")

        val result =
            analyzeMethodRecursively(
                targetMethod,
                maxDepth,
                mutableSetOf(),
            )

        return MethodCallResult(
            qualifiedName = targetMethod.fullName,
            calledMethods = result.map { it.targetName }.distinct(),
        )
    }

    private fun analyzeMethodRecursively(
        method: JavaMethod,
        depth: Int,
        analyzedMethods: MutableSet<String>,
    ): List<CallInfo> {
        if (depth <= 0) return emptyList()

        if (!analyzedMethods.add(method.fullName)) {
            return emptyList()
        }

        val calledMethods =
            buildList {
                // アノテーションの収集
                method.annotations.forEach { annotation ->
                    val targetPackageName = FqName(annotation.type.name.substringBeforeLast("."))
                    if (isTargetPackage(targetPackageName)) {
                        add(
                            CallInfo.Annotation(
                                targetName = annotation.type.name,
                                ownerClass = annotation.type.name,
                            ),
                        )
                    }
                }

                // メソッドが定義されているクラスのアノテーションも収集
                method.owner.annotations.forEach { annotation ->
                    val targetPackageName = FqName(annotation.type.name.substringBeforeLast("."))
                    if (isTargetPackage(targetPackageName)) {
                        add(
                            CallInfo.Annotation(
                                targetName = annotation.type.name,
                                ownerClass = annotation.type.name,
                            ),
                        )
                    }
                }

                // メソッド呼び出しの収集
                method.methodCallsFromSelf.forEach { call ->
                    val targetPackageName = FqName(call.target.owner.packageName)
                    if (isTargetPackage(targetPackageName)) {
                        add(
                            CallInfo.RegularMethodCall(
                                targetName = call.target.fullName,
                                ownerClass = call.target.owner.name,
                                methodName = call.target.name,
                                parameterTypes = call.target.parameterTypes.map { it.name },
                            ),
                        )

                        // 解決可能なメソッドのみ再帰的に解析
                        call.target.resolveMember().ifPresent { resolvedMethod ->
                            val subResult =
                                analyzeMethodRecursively(
                                    resolvedMethod,
                                    if (resolvedMethod.modifiers.contains(JavaModifier.PRIVATE)) depth else depth - 1,
                                    analyzedMethods,
                                )
                            addAll(subResult)
                        }
                    }
                }

                // コンストラクタ呼び出しの収集
                method.constructorCallsFromSelf.forEach { call ->
                    val targetPackageName = FqName(call.target.owner.packageName)
                    if (isTargetPackage(targetPackageName)) {
                        add(
                            CallInfo.ConstructorCall(
                                targetName = call.target.fullName,
                                ownerClass = call.target.owner.name,
                                parameterTypes = call.target.parameterTypes.map { it.name },
                            ),
                        )
                    }
                }

                // メソッド参照の収集
                method.methodReferencesFromSelf.forEach { ref ->
                    val targetPackageName = FqName(ref.target.owner.packageName)
                    if (isTargetPackage(targetPackageName)) {
                        add(
                            CallInfo.MethodReference(
                                targetName = ref.target.fullName,
                                ownerClass = ref.target.owner.name,
                                methodName = ref.target.name,
                            ),
                        )

                        // 解決可能なメソッドのみ再帰的に解析
                        ref.target.resolveMember().ifPresent { resolvedMethod ->
                            val subResult =
                                analyzeMethodRecursively(
                                    resolvedMethod,
                                    if (resolvedMethod.modifiers.contains(JavaModifier.PRIVATE)) depth else depth - 1,
                                    analyzedMethods,
                                )
                            addAll(subResult)
                        }
                    }
                }
            }

        return calledMethods
    }

    /**
     * 指定されたパッケージが解析対象かどうかを判定します。完全一致する場合のみ対象とする
     *
     * @param packageName 判定対象のパッケージ名
     * @return 解析対象の場合はtrue、それ以外はfalse
     */
    private fun isTargetPackage(packageName: FqName): Boolean = targetPackages.contains(packageName)
}
