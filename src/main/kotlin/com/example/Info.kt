package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

/**
 * 指定されたKotlin関数の完全修飾名を、パラメータ型を含めて構築します。
 *
 * @param function 完全修飾名を構築したいKotlin関数（`KtFunction`）
 * @return パラメータ型情報を括弧内に含めた関数の完全修飾名を表す文字列
 */
fun toQualifiedName(function: KtFunction): String {
    val parameterTypes = function.valueParameters.map { toTypeName(it) }
    val parameterString = "(${parameterTypes.joinToString(",")})"
    return "${function.fqName?.asString() ?: ""}$parameterString"
}

/**
 * 指定された Kotlin パラメータの型リファレンスを、対応する型名として文字列に変換します。
 *
 * @param parameter 型を変換したい Kotlin パラメータ。
 * @return 指定されたパラメータの型名を表す文字列。型リファレンスが存在しない場合は "Any" を返します。
 */
fun toTypeName(parameter: KtParameter): String {
    return parameter.typeReference?.javaTypeString() ?: "Any"
}

/**
 * メソッドの情報を表すデータクラス
 *
 * @property name メソッド名
 * @property qualifiedName メソッドの完全修飾名
 * @property visibility 可視性（public, private, protected, internal）
 * @property annotations メソッドに付与されているアノテーション
 * @property modifiers メソッドの修飾子リスト（override, abstractなど）
 * @property parameters メソッドのパラメータ情報リスト
 * @property returnType 戻り値の型
 */
@Serializable
data class MethodInfo(
    val name: String,
    val qualifiedName: String,
    val visibility: String,
    val annotations: List<String>,
    val modifiers: List<String>,
    val parameters: List<ParameterInfo>,
    val returnType: String,
) {
    companion object {
        /**
         * KtFunctionからメソッド情報を抽出します。
         */
        fun from(function: KtFunction): MethodInfo {
            val allModifiers = function.modifierList?.text?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
            val (annotations, modifiers) = allModifiers.partition { it.startsWith("@") }

            return MethodInfo(
                name = function.name ?: "",
                qualifiedName = toQualifiedName(function),
                visibility = function.visibilityModifierType()?.value ?: "public",
                annotations = annotations.map { it.trim() }.sorted(),
                modifiers = modifiers.sorted(),
                parameters = function.valueParameters.map { ParameterInfo.from(it) },
                returnType = function.typeReference?.javaTypeString() ?: "Unit",
            )
        }
    }
}

/**
 * メソッドのパラメータ情報を表すデータクラス
 *
 * @property name パラメータ名
 * @property type パラメータの型
 * @property defaultValue デフォルト値（存在する場合）
 */
@Serializable
data class ParameterInfo(
    val name: String,
    val type: String,
    // デフォルト値（存在する場合）
    val defaultValue: String? = null,
) {
    companion object {
        /**
         * KtParameterからパラメータ情報を抽出します。
         */
        fun from(parameter: KtParameter): ParameterInfo {
            return ParameterInfo(
                name = parameter.name ?: "",
                type = toTypeName(parameter),
                defaultValue = parameter.defaultValue?.text,
            )
        }
    }
}
