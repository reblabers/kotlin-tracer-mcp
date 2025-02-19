package com.example

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import java.nio.file.Paths

// KtNodeTypes に種類が載っている
// import org.jetbrains.kotlin.KtNodeTypes.BINARY_EXPRESSION

fun <T : PsiElement> T.classOrInterfaceList(): List<KtClass> {
    return PsiTreeUtil.findChildrenOfType(this, KtClass::class.java).toList()
}

fun <T : PsiElement> T.functionList(): List<KtFunction> {
    return PsiTreeUtil.findChildrenOfType(this, KtFunction::class.java).toList()
}

fun <T : PsiElement> T.callList(): List<KtCallExpression> {
    return PsiTreeUtil.findChildrenOfType(this, KtCallExpression::class.java).toList()
}

fun <T : PsiElement> T.binaryExpressionList(): List<KtBinaryExpression> {
    return PsiTreeUtil.findChildrenOfType(this, KtBinaryExpression::class.java).toList()
}

fun <T : PsiElement> T.elementType(): IElementType {
    return this.node.elementType
}

fun KtFile.composePsiFile(factory: PsiFileFactory): PsiFile? {
//    return factory.createFileFromText(this.name.toString(), KotlinLanguage.INSTANCE, text)
//    return factory.createFileFromText(text, this.originalFile)
    return factory.createFileFromText(KotlinLanguage.INSTANCE, text)
}

fun KtTypeReference.javaTypeString(): String {
    return when (text) {
        "Int" -> "int"
        "Long" -> "long"
        "Double" -> "double"
        "Float" -> "float"
        "Boolean" -> "boolean"
        "Char" -> "char"
        "Byte" -> "byte"
        "Short" -> "short"
        else -> text
    }
}

@ConsistentCopyVisibility
data class SourceFile private constructor(
    private val file: KtFile,
    private val copyToPsiFile: (KtFile) -> PsiFile,
) {
    companion object {
        fun load(
            file: KtFile,
            factory: PsiFileFactory,
        ): SourceFile {
            return SourceFile(file) { it.composePsiFile(factory) ?: error("") }
        }
    }

    val packageFqName get() = file.packageFqName

    val path get() = Paths.get(file.virtualFile.path)

    val name get() = file.name

    val readonly get() = file

    val writable: PsiFile by lazy { copyToPsiFile(file) }

    fun accept(visitor: PsiElementVisitor) {
        file.accept(visitor)
    }

    fun save() {
        path.toFile().writeText(writable.text)
    }
}

data class SourceFiles(val files: List<SourceFile>) {
    fun inside(fqName: FqName): SourceFiles {
        return SourceFiles(files.filter { it.packageFqName == fqName || it.packageFqName.isInsideOf(fqName) })
    }

    fun packages(): Set<FqName> {
        return files.map { it.packageFqName }.toSet()
    }

    fun filter(predicate: (SourceFile) -> Boolean): SourceFiles {
        return SourceFiles(files.filter(predicate))
    }
}

fun FqName.isInsideOf(parent: FqName) = this != parent && this.startsWith(parent)

/**
 * thisが、指定された親パッケージの配下にあるか、または同じであるかを判定します。
 *
 * @param parent 親パッケージ
 * @return thisが親パッケージの配下にあるか、同じ場合はtrue、そうでない場合はfalse
 */
fun FqName.isOrInsideOf(parent: FqName): Boolean = this.startsWith(parent)
