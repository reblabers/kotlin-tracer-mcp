package com.example

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val format = Json { prettyPrint = true }

/**
 * 実行エラーを表すデータクラス
 *
 * @property error エラーメッセージ
 */
@Serializable
data class ErrorResult(
    val error: ErrorInfo,
)

@Serializable
data class ErrorInfo(
    val type: String,
    val message: String,
)

fun handling(block: (CallToolRequest) -> List<PromptMessageContent>): (CallToolRequest) -> CallToolResult =
    { request ->
        try {
            val content = block(request)
            CallToolResult(
                content = content,
                isError = false,
            )
        } catch (e: Exception) {
            val errorInfo =
                ErrorInfo(
                    type = e.javaClass.name,
                    message = e.message ?: "An error occurred",
                )
            CallToolResult(
                content =
                    listOf(
                        TextContent(format.encodeToString(ErrorResult(errorInfo))),
                    ),
                isError = true,
            )
        }
    }

fun configureServer(): Server {
    val allProjects =
        mapOf(
            "mvc-kotlin" to Projects.MvcKotlin,
        )

    val projectEnums = allProjects.map { it.key }.joinToString(",") { "\"$it\"" }

    val server =
        Server(
            Implementation(
                name = "kotlin-tracer",
                version = "1.0.0",
            ),
            ServerOptions(
                capabilities =
                    ServerCapabilities(
                        tools = ServerCapabilities.Tools(listChanged = null),
                    ),
            ),
        )

    server.addTool(
        name = "get_total_source_files",
        description = "Returns the total number of source files in the project.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it. The available project names are defined in the enum list."
                    }
                  }
                }
                """,
                ).jsonObject,
                required = listOf("project"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val counter = ToolFactory.forProject(project).createSourceFileCounter()
                val result = counter.getTotalSourceFiles()
                listOf(TextContent(format.encodeToString<GetTotalSourceFilesResult>(result)))
            },
    )

    server.addTool(
        name = "search_files",
        description = "Search for files in the project using a regex pattern.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it."
                    },
                    "pattern": {
                      "type": "string",
                      "description": "Regular expression pattern to match file names."
                    },
                    "offset": {
                      "type": "integer",
                      "description": "Pagination offset.",
                      "default": 0
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Number of results per page.",
                      "default": 50
                    },
                    "maxResults": {
                      "type": "integer",
                      "description": "Maximum number of results to return.",
                      "default": 100
                    },
                    "fullPath": {
                      "type": "boolean",
                      "description": "Whether to return full paths or just file names.",
                      "default": false
                    }
                  }
                }
                """,
                ).jsonObject,
                required = listOf("project"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val searcher = ToolFactory.forProject(project).createFileSearcher()
                val result =
                    searcher.searchFiles(
                        FileSearchRequest(
                            pattern = request.arguments["pattern"]?.jsonPrimitive?.content,
                            offset = request.arguments["offset"]?.jsonPrimitive?.content?.toInt() ?: 0,
                            limit = request.arguments["limit"]?.jsonPrimitive?.content?.toInt() ?: 50,
                            maxResults = request.arguments["maxResults"]?.jsonPrimitive?.content?.toInt() ?: 100,
                            fullPath = request.arguments["fullPath"]?.jsonPrimitive?.content?.toBoolean() == true,
                        ),
                    )
                listOf(TextContent(format.encodeToString(result)))
            },
    )

    server.addTool(
        name = "search_class_names",
        description = "Search for class names in the project using a regex pattern.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it."
                    },
                    "pattern": {
                      "type": "string",
                      "description": "Regular expression pattern to match class names."
                    },
                    "offset": {
                      "type": "integer",
                      "description": "Pagination offset.",
                      "default": 0
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Number of results per page.",
                      "default": 50
                    },
                    "maxResults": {
                      "type": "integer",
                      "description": "Maximum number of results to return.",
                      "default": 100
                    },
                    "fullPath": {
                      "type": "boolean",
                      "description": "Whether to return full paths or just file names.",
                      "default": false
                    }
                  }
                }
                """,
                ).jsonObject,
                required = listOf("project"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val searcher = ToolFactory.forProject(project).createClassNameSearcher()

                // リクエストパラメータを取得（デフォルト値を使用）
                val offset = request.arguments["offset"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val limit = request.arguments["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50
                val maxResults = request.arguments["maxResults"]?.jsonPrimitive?.content?.toIntOrNull() ?: 100
                val fullPath = request.arguments["fullPath"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() == true

                val result =
                    searcher.searchClassNames(
                        ClassNameSearchRequest(
                            pattern = request.arguments["pattern"]?.jsonPrimitive?.content,
                            offset = offset,
                            limit = limit,
                            maxResults = maxResults,
                            fullPath = fullPath,
                        ),
                    )
                listOf(TextContent(format.encodeToString(result)))
            },
    )

    server.addTool(
        name = "analyze_class_methods",
        description = "Analyze methods of a specified class.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it."
                    },
                    "className": {
                      "type": "string",
                      "description": "The name of the class to analyze (simple name or fully qualified name)."
                    }
                  }
                }
                """,
                ).jsonObject,
                required = listOf("project", "className"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val analyzer = ToolFactory.forProject(project).createClassMethodAnalyzer()
                val className = requireNotNull(request.arguments["className"]).jsonPrimitive.content

                val result =
                    if (className.contains(".")) {
                        analyzer.analyzeByFqName(className)
                    } else {
                        analyzer.analyzeBySimpleName(className)
                    }

                listOf(TextContent(format.encodeToString(result)))
            },
    )

    server.addTool(
        name = "analyze_method_calls",
        description = "Analyze method calls from a specific method.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it."
                    },
                    "qualifiedName": {
                      "type": "string",
                      "description": "The fully qualified name of the method to analyze (e.g. 'org.example.MyClass.myMethod(int,long)'). For top-level functions, use the package name followed by the function name."
                    },
                    "maxDepth": {
                      "type": "integer",
                      "description": "Maximum depth of method call analysis (default: 5).",
                      "default": 5
                    }
                  },
                  "required": ["project", "qualifiedName"]
                }
                """,
                ).jsonObject,
                required = listOf("project", "qualifiedName"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val analyzer = ToolFactory.forProject(project).createMethodCallAnalyzer()
                val qualifiedName = requireNotNull(request.arguments["qualifiedName"]).jsonPrimitive.content
                val maxDepth = request.arguments["maxDepth"]?.jsonPrimitive?.content?.toInt() ?: 5

                val result = analyzer.analyzeMethodCalls(qualifiedName, project.searchScope, maxDepth)
                listOf(TextContent(format.encodeToString(result)))
            },
    )

    server.addTool(
        name = "get_methods",
        description = "Get detailed information about specific methods including their source code.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it."
                    },
                    "qualifiedNames": {
                      "type": "array",
                      "items": {
                        "type": "string",
                        "description": "The fully qualified name of the method to analyze (e.g. 'org.example.MyClass.myMethod(int,long)'). For top-level functions, use the package name followed by the function name."
                      }
                    }
                  },
                  "required": ["project", "qualifiedNames"]
                }
                """,
                ).jsonObject,
                required = listOf("project", "qualifiedNames"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val analyzer = ToolFactory.forProject(project).createMethodDetailAnalyzer()
                val qualifiedNames =
                    requireNotNull(request.arguments["qualifiedNames"]).jsonArray.map { it.jsonPrimitive.content }

                val result = analyzer.getMethodDetails(qualifiedNames)
                listOf(
                    TextContent(format.encodeToString(result)),
                )
            },
    )

    server.addTool(
        name = "get_classes",
        description = "Get detailed information about specific classes including their source code.",
        inputSchema =
            Tool.Input(
                Json.parseToJsonElement(
                    """
                {
                  "type":"object",
                  "properties":{
                    "project": {
                      "type": "string",
                      "enum": [$projectEnums],
                      "description": "Specifies the name of the project to identify it."
                    },
                    "qualifiedNames": {
                      "type": "array",
                      "items": {
                        "type": "string",
                        "description": "The fully qualified name of the class to analyze (e.g., 'org.example.MyClass')."
                      }
                    }
                  },
                  "required": ["project", "qualifiedNames"]
                }
                """,
                ).jsonObject,
                required = listOf("project", "qualifiedNames"),
            ),
        handler =
            handling { request ->
                val projectName = requireNotNull(request.arguments["project"]).jsonPrimitive.content
                val project = requireNotNull(allProjects[projectName])
                val analyzer = ToolFactory.forProject(project).createClassDetailAnalyzer()
                val qualifiedNames =
                    requireNotNull(request.arguments["qualifiedNames"]).jsonArray.map { it.jsonPrimitive.content }

                val result = analyzer.getClassDetails(qualifiedNames)
                listOf(
                    TextContent(format.encodeToString(result)),
                )
            },
    )

    return server
}
