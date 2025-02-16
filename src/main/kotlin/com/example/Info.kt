package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

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
            val parameterTypes = function.valueParameters.map { ParameterInfo.from(it).type }
            val parameterString = "(${parameterTypes.joinToString(",")})"
            val allModifiers = function.modifierList?.text?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
            val (annotations, modifiers) = allModifiers.partition { it.startsWith("@") }

            return MethodInfo(
                name = function.name ?: "",
                qualifiedName = "${function.fqName?.asString() ?: ""}$parameterString",
                visibility = function.visibilityModifierType()?.value ?: "public",
                annotations = annotations.sorted(),
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
                type = parameter.typeReference?.javaTypeString() ?: "Any",
                defaultValue = parameter.defaultValue?.text,
            )
        }
    }
}
