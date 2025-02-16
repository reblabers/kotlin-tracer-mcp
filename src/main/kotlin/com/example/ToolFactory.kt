package com.example

import java.nio.file.Path

class ToolFactory private constructor(
    private val resources: Resources,
) {
    fun createSourceFileCounter(): SourceFileCounter {
        return SourceFileCounter(resources)
    }

    fun createFileSearcher(): FileSearcher {
        return FileSearcher(resources)
    }

    fun createClassNameSearcher(): ClassNameSearcher {
        return ClassNameSearcher(resources)
    }

    fun createClassMethodAnalyzer(): ClassMethodAnalyzer {
        val classNameSearcher = createClassNameSearcher()
        return ClassMethodAnalyzer(resources, classNameSearcher)
    }

    fun createMethodCallAnalyzer(): MethodCallAnalyzer {
        return MethodCallAnalyzer(resources)
    }

    fun createMethodDetailAnalyzer(): MethodDetailAnalyzer {
        return MethodDetailAnalyzer(resources)
    }

    fun createClassDetailAnalyzer(): ClassDetailAnalyzer {
        return ClassDetailAnalyzer(resources)
    }

    companion object {
        fun forProject(project: Projects): ToolFactory {
            return ToolFactory(project.resources)
        }

        fun forPath(path: Path): ToolFactory {
            return ToolFactory(EnvironmentProvider.create(path))
        }

        fun forPathWithJar(
            path: Path,
            jarPath: Path,
        ): ToolFactory {
            return forPaths(listOf(path), listOf(jarPath))
        }

        fun forPaths(
            paths: List<Path>,
            jarPaths: List<Path> = emptyList(),
        ): ToolFactory {
            return ToolFactory(EnvironmentProvider.create(paths, jarPaths))
        }
    }
}
