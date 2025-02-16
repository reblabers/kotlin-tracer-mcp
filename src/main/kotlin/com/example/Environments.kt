package com.example

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiElementFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import sun.reflect.ReflectionFactory
import java.net.URLClassLoader
import java.nio.file.Path

enum class Projects(
    private val paths: List<Path>,
    private val jarPaths: List<Path> = emptyList(),
    val searchScope: String,
) {
    MvcKotlin(
        paths = listOf(Path.of("repositories/mvc-kotlin/src/main/kotlin")),
        jarPaths = listOf(Path.of("repositories/mvc-kotlin/build/libs/mvc-kotlin-0.0.1-plain.jar")),
        searchScope = "org.example",
    ),
    ;

    val resources: Resources by lazy { EnvironmentProvider.create(paths, jarPaths) }
}

class Resources(
    private val environment: KotlinCoreEnvironment,
    private val sources: SourceFiles,
) {
    private val classFileImporter: ClassFileImporter by lazy {
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
    }

    fun allSources() = sources

    fun project() = environment.project

    val psiFactory by lazy { KtPsiFactory(project()) }

    val psiFileFactory by lazy { PsiFileFactory.getInstance(project()) }

    val psiElementFactory by lazy { PsiElementFactory.getInstance(project()) }

    /**
     * 指定されたパッケージ名に属するすべてのクラスを取得します
     * @param packageName パッケージ名
     * @return パッケージに属するJavaClassのリスト
     */
    fun classesInPackage(packageName: String): List<JavaClass> {
        return classFileImporter.importPackages(packageName).toList()
    }

    /**
     * プロジェクト内のすべてのファイルを取得します
     * @return プロジェクト内のファイルパスのリスト
     */
    fun getFileList(): List<String> {
        return sources.files.map { it.path.toString() }
            .filter { path ->
                // 隠しファイルを除外
                !path.split("/").any { it.startsWith(".") }
            }
    }
}

object EnvironmentProvider {
    /**
     * 単一のパスからKotlin環境とリソースを作成する
     */
    fun create(sourcePath: Path): Resources = create(listOf(sourcePath))

    /**
     * 指定されたパスからKotlin環境とリソースを作成する
     */
    fun create(
        sourcePaths: List<Path>,
        jarPaths: List<Path> = emptyList(),
    ): Resources {
        val environment =
            KotlinCoreEnvironment.createForProduction(
                Disposer.newDisposable(),
                CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )

        sourcePaths.forEach {
            require(it.toFile().exists()) { "Source path does not exist - $it" }
        }

        jarPaths.forEach {
            require(it.toFile().exists()) { "JAR file does not exist - $it" }
        }

        environment.addKotlinSourceRoots(sourcePaths.map { it.toFile() })

        val mockProject =
            requireNotNull(environment.project as? MockProject) {
                "MockProject type expected, actual - ${environment.project.javaClass.simpleName}"
            }
        mockProject.registerService(PomModel::class.java, DummyPomModel)

        val extension = TreeCopyHandler::class.qualifiedName.toString()
        val extensionClass = TreeCopyHandler::class.java.name
        val extensionArea = mockProject.extensionArea
        synchronized(extensionArea) {
            if (!extensionArea.hasExtensionPoint(extension)) {
                @Suppress("DEPRECATION")
                extensionArea.registerExtensionPoint(extension, extensionClass, ExtensionPoint.Kind.INTERFACE)
            }
        }

        val psiFileFactory = PsiFileFactory.getInstance(mockProject)
        val sources = SourceFiles(environment.getSourceFiles().map { SourceFile.load(it, psiFileFactory) })

        // JARファイルをクラスパスに追加
        val currentThread = Thread.currentThread()
        synchronized(currentThread) {
            val classLoader = currentThread.contextClassLoader
            val jarUrls = jarPaths.map { it.toUri().toURL() }.toTypedArray()
            val urlClassLoader = URLClassLoader(jarUrls, classLoader)
            currentThread.contextClassLoader = urlClassLoader
        }

        return Resources(environment, sources)
    }

    // https://github.com/detekt/detekt/blob/551ef6c43e1a716df086dd12c9bea93e57847643/detekt-parser/src/main/kotlin/io/github/detekt/parser/DetektPomModel.kt#L15
    object DummyPomModel : UserDataHolderBase(), PomModel {
        private fun readResolve(): Any = DummyPomModel

        override fun runTransaction(transaction: PomTransaction) {
            val transactionCandidate = transaction as? PomTransactionBase

            val pomTransaction =
                requireNotNull(transactionCandidate) {
                    "${PomTransactionBase::class.simpleName} type expected, actual is ${transaction.javaClass.simpleName}"
                }

            pomTransaction.run()
        }

        override fun <T : PomModelAspect?> getModelAspect(aspect: Class<T>): T? {
            if (aspect == TreeAspect::class.java) {
                val constructor =
                    ReflectionFactory.getReflectionFactory()
                        .newConstructorForSerialization(aspect, Any::class.java.getDeclaredConstructor())
                @Suppress("UNCHECKED_CAST")
                return constructor.newInstance() as T
            }
            return null
        }
    }
}
