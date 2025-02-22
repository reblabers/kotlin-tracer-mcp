package com.example

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaConstructor
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters

class Finder(
    private val resources: Resources,
    private val methodSearchScope: String,
) {
    private val ktFiles by lazy {
        val targetFqName = FqName(methodSearchScope)
        resources.allSources().files.filter { it.packageFqName.isOrInsideOf(targetFqName) }
    }

    private val ktFunctions by lazy {
        ktFiles.flatMap { it.readonly.functionList() }
    }

    private val ktClassesOrInterfaces by lazy {
        ktFiles.flatMap { it.readonly.classOrInterfaceList() }
    }

    private val javaClasses by lazy {
        resources.classesInPackage(methodSearchScope)
    }

    private val javaMethods by lazy {
        javaClasses.flatMap { it.methods }
    }

    /**
     * 指定された名前の関数を検索します。
     *
     * @param qualifiedMethodName 関数の完全修飾名 (KtFunctionのみ)
     * @return 見つかった関数、見つからない場合はnull
     */
    fun findKtFunction(qualifiedMethodName: String): KtFunction? {
        val targetFqName = FqName(qualifiedMethodName)
        return ktFiles
            .filter { targetFqName.isOrInsideOf(it.packageFqName) }
            .flatMap { it.readonly.functionList() }
            .find { toQualifiedName(it) == qualifiedMethodName }
    }

    /**
     * 指定された名前のメソッドを検索します。
     *
     * @param qualifiedMethodName メソッドの完全修飾名 (JavaMethodのみ)
     * @return 見つかったメソッド、見つからない場合はnull
     */
    fun findJavaMethod(qualifiedMethodName: String): JavaMethod? {
        val targetFqName = FqName(qualifiedMethodName)
        return javaClasses
            .filter { targetFqName.isOrInsideOf(FqName(it.packageName)) }
            .flatMap { it.methods }
            .find { it.fullName == qualifiedMethodName }
    }

    /**
     * 指定された Java メソッドに対応する Kotlin 関数を検索します。
     *
     * @param javaMethod 検索対象の Java メソッド
     * @return 見つかった Kotlin 関数、見つからない場合は null
     */
    fun findKtFunction(javaMethod: JavaMethod): KtFunction? {
        val functions =
            ktFunctions
                .filter { it.name == javaMethod.name }
                .toList()

        if (functions.size == 1) {
            return functions.single()
        }

        if (functions.isNotEmpty()) {
            val targetParameterTypes = toStringsFromJava(javaMethod.parameterTypes)
            functions
                .find { toStringsFromKt(it.valueParameters) == targetParameterTypes }
                ?.let { return it }
        }

        return null
    }

    /**
     * 指定された Kotlin 関数に対応する Java メソッドを検索します。
     *
     * @param ktFunction 検索対象の Kotlin 関数
     * @return 見つかった Java メソッド、見つからない場合は null
     */
    fun findJavaMethod(ktFunction: KtFunction): JavaMethod? {
        val methods = javaMethods.filter { it.name == ktFunction.name }

        if (methods.size == 1) {
            return methods.single()
        }

        if (methods.isNotEmpty()) {
            val targetParameterTypes = toStringsFromKt(ktFunction.valueParameters)
            methods
                .find { toStringsFromJava(it.parameterTypes) == targetParameterTypes }
                ?.let { return it }
        }

        return null
    }

    /**
     * 指定された名前のクラスを検索します。
     *
     * @param qualifiedClassName クラスの完全修飾名 (KtClass/JavaClass両対応)
     * @return 見つかったクラス、見つからない場合はnull
     */
    fun findKtClass(qualifiedClassName: String): KtClass? {
        val targetClassName = qualifiedClassName.replace("$", ".")
        val targetFqName = FqName(targetClassName)
        return ktFiles
            .filter { targetFqName.isOrInsideOf(it.packageFqName) }
            .flatMap { it.readonly.classOrInterfaceList() }
            .firstOrNull { it.fqName == targetFqName }
    }

    /**
     * 指定された Kotlin クラスを検索します。
     *
     * @param qualifiedClassName クラスの完全修飾名 (KtClass/JavaClass両対応)
     * @return 見つかったクラス、見つからない場合はnull
     */
    fun findJavaClass(qualifiedClassName: String): JavaClass? {
        val targetClassName = qualifiedClassName.replace("$", ".")
        return javaClasses.find { it.name.replace("$", ".") == targetClassName }
    }

    /**
     * 指定された Java クラスを検索します。
     *
     * @param javaClazz 検索対象の Java クラス
     * @return 見つかったクラス、見つからない場合はnull
     */
    fun findKtClass(javaClazz: JavaClass): KtClass? {
        val targetClassName = javaClazz.name.replace("$", ".")
        return ktClassesOrInterfaces.find {
            it.fqName?.asString() == targetClassName
        }
    }

    /**
     * 指定された Kotlin クラスを検索します。
     *
     * @param ktClass 検索対象の Kotlin クラス
     * @return 見つかったクラス、見つからない場合はnull
     */
    fun findJavaClass(ktClass: KtClass): JavaClass? {
        val targetClassName = ktClass.fqName?.asString() ?: return null
        return javaClasses.find { it.name.replace("$", ".") == targetClassName }
    }

    /**
     * 指定された名前のプロパティを検索します。
     *
     * @param qualifiedPropertyName プロパティの完全修飾名 (KtClass/JavaClass両対応)
     * @return 見つかったプロパティ、見つからない場合はnull
     */
    fun findKtParameter(qualifiedPropertyName: String): KtParameter? {
        val targetFqName = FqName(qualifiedPropertyName.replace("$", "."))
        return findKtClass(targetFqName.parent().asString())?.let { ktClass ->
            return ktClass.getValueParameters().firstOrNull { it.fqName == targetFqName }
        }
    }

    /**
     * 指定された完全修飾名の Kotlin プロパティを検索します。
     *
     * @param qualifiedPropertyName プロパティの完全修飾名 (KtClass/JavaClass両対応)
     * @return 見つかったプロパティを [KtProperty] として返します。見つからない場合は null を返します。
     */
    fun findKtProperty(qualifiedPropertyName: String): KtProperty? {
        val targetFqName = FqName(qualifiedPropertyName)
        return findKtClass(targetFqName.parent().asString())?.let { ktClass ->
            val properties = ktClass.getProperties()
            return properties.firstOrNull { it.fqName == targetFqName }
        }
    }

    fun findJavaConstructor(qualifiedMethodName: String): JavaConstructor? {
        val targetFqName = FqName(qualifiedMethodName)
        return javaClasses
            .filter { targetFqName.isOrInsideOf(FqName(it.packageName)) }
            .flatMap { it.constructors }
            .find { it.fullName == qualifiedMethodName }
    }

    fun findKtConstructor(qualifiedPropertyName: String): KtConstructor<*>? {
        val javaConstructor = findJavaConstructor(qualifiedPropertyName)
        if (javaConstructor == null) {
            return null
        }

        val targetParameterTypes = toStringsFromJava(javaConstructor.parameterTypes)
        findKtClass(javaConstructor.owner)?.let { ktClass ->
            ktClass.constructors()
                .find { toStringsFromKt(it.valueParameters) == targetParameterTypes }
                ?.let { return it }
        }

        return null
    }
}

private fun toStringsFromKt(parameters: List<KtParameter>): List<String> =
    parameters
        .map { toTypeName(it) }
        .map { it.substringAfterLast(".") }
        .map { it.lowercase() }

private fun toStringsFromJava(parameters: List<JavaType>): List<String> =
    parameters
        .map { it.name }
        .map { it.substringAfterLast(".") }
        .map { it.lowercase() }
