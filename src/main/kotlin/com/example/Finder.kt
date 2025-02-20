package com.example

import com.tngtech.archunit.core.domain.JavaMethod
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction

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

    private val javaClasses by lazy {
        resources.classesInPackage(methodSearchScope)
    }

    fun findKtFunction(qualifiedMethodName: String): KtFunction? {
        val targetFqName = FqName(qualifiedMethodName)
        return ktFiles
            .filter { targetFqName.isOrInsideOf(it.packageFqName) }
            .flatMap { it.readonly.functionList() }
            .find { toQualifiedName(it) == qualifiedMethodName }
    }

    fun findJavaMethod(qualifiedMethodName: String): JavaMethod? {
        val targetFqName = FqName(qualifiedMethodName)
        return javaClasses
            .filter { targetFqName.isOrInsideOf(FqName(it.packageName)) }
            .flatMap { it.methods }
            .find { it.fullName == qualifiedMethodName }
    }

    fun findKtFunction(javaMethod: JavaMethod): KtFunction? {
        val functions =
            ktFunctions
                .filter { it.name == javaMethod.name }
                .toList()

        if (functions.size == 1) {
            return functions.single()
        }

        if (functions.isNotEmpty()) {
            val targetParameterTypes =
                javaMethod.parameterTypes
                    .map { it.name }
                    .map { it.substringAfterLast(".") }
                    .map { it.lowercase() }

            for (function in functions) {
                val parameterTypes =
                    function.valueParameters
                        .map { toTypeName(it) }
                        .map { it.substringAfterLast(".") }
                        .map { it.lowercase() }
                if (parameterTypes == targetParameterTypes) {
                    return function
                }
            }
        }

        return null
    }

    fun findJavaMethod(ktFunction: KtFunction): JavaMethod? {
        val methods =
            javaClasses
                .flatMap { it.methods }
                .filter { it.name == ktFunction.name }

        if (methods.size == 1) {
            return methods.single()
        }

        if (methods.isNotEmpty()) {
            val targetParameterTypes =
                ktFunction.valueParameters
                    .map { toTypeName(it) }
                    .map { it.substringAfterLast(".") }
                    .map { it.lowercase() }

            for (javaMethod in methods) {
                val parameterTypes =
                    javaMethod.parameterTypes
                        .map { it.name }
                        .map { it.substringAfterLast(".") }
                        .map { it.lowercase() }

                if (parameterTypes == targetParameterTypes) {
                    return javaMethod
                }
            }
        }

        return null
    }
}
