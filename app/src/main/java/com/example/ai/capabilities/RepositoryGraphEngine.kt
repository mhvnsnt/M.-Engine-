package com.example.ai.capabilities

import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class SymbolKind {
    CLASS,
    INTERFACE,
    OBJECT,
    FUNCTION,
    PROPERTY,
    MODULE,
    GRADLE_CONFIG
}

data class CodeSymbol(
    val qualifiedName: String,
    val simpleName: String,
    val kind: SymbolKind,
    val filePath: String,
    val packageName: String,
    val line: Int = 1,
    val superTypes: List<String> = emptyList(),
    val functions: List<String> = emptyList(),
    val properties: List<String> = emptyList()
)

data class FileAnalysis(
    val filePath: String,
    val packageName: String,
    val imports: List<String>,
    val symbols: List<CodeSymbol>,
    val functionCalls: List<String>,
    val isTestFile: Boolean,
    val hash: String,
    val lastModified: Long
)

data class ModuleNode(
    val name: String,
    val path: String,
    val dependencies: List<String>,
    val plugins: List<String>,
    val isAppModule: Boolean
)

data class CrossRepoLink(
    val sourceRepo: String,
    val targetRepo: String,
    val relationship: String,
    val filePath: String
)

data class RepositoryGraph(
    val rootDir: String,
    val modules: Map<String, ModuleNode> = emptyMap(),
    val files: Map<String, FileAnalysis> = emptyMap(),
    val symbols: Map<String, CodeSymbol> = emptyMap(),
    val callersMap: Map<String, List<String>> = emptyMap(),
    val inheritanceMap: Map<String, List<String>> = emptyMap(),
    val testToFileMap: Map<String, List<String>> = emptyMap(),
    val crossRepoLinks: List<CrossRepoLink> = emptyList(),
    val totalFilesIndexed: Int = 0,
    val indexedTimestamp: Long = System.currentTimeMillis()
)

interface RepositoryGraphEngine {
    fun indexRepository(rootDir: File): RepositoryGraph
    fun updateFile(filePath: String, content: String): RepositoryGraph
    fun invalidateFile(filePath: String): RepositoryGraph
    fun getGraph(): RepositoryGraph
    fun findImpactedComponents(changedFiles: List<String>): List<String>
    fun findAssociatedTests(componentNameOrPath: String): List<String>
    fun findCallers(symbolName: String): List<String>
    fun findDependencies(symbolOrModule: String): List<String>
}

class RepositoryGraphEngineImpl(
    private var rootDirectory: File = File(".")
) : RepositoryGraphEngine {

    private val filesMap = ConcurrentHashMap<String, FileAnalysis>()
    private val modulesMap = ConcurrentHashMap<String, ModuleNode>()
    private val crossRepoLinks = mutableListOf<CrossRepoLink>()
    private var currentGraph: RepositoryGraph = RepositoryGraph(rootDir = rootDirectory.absolutePath)

    override fun indexRepository(rootDir: File): RepositoryGraph {
        rootDirectory = rootDir
        filesMap.clear()
        modulesMap.clear()
        crossRepoLinks.clear()

        // 1. Scan for Gradle module structures
        scanModules(rootDir)

        // 2. Scan and parse all Kotlin/Java and build files
        if (rootDir.exists()) {
            rootDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.name.endsWith(".gradle.kts")) }
                .filterNot { it.path.contains("/build/") || it.path.contains("/.gradle/") }
                .forEach { file ->
                    val relativePath = file.relativeToOrSelf(rootDir).path
                    val content = try { file.readText() } catch (_: Exception) { "" }
                    val analysis = parseFile(relativePath, content, file.lastModified())
                    filesMap[relativePath] = analysis
                }
        }

        // 3. Scan cross-repository relationships (e.g. mhvnsnt/M.-Engine-)
        detectCrossRepoLinks()

        return rebuildGraph()
    }

    override fun updateFile(filePath: String, content: String): RepositoryGraph {
        val analysis = parseFile(filePath, content, System.currentTimeMillis())
        filesMap[filePath] = analysis
        return rebuildGraph()
    }

    override fun invalidateFile(filePath: String): RepositoryGraph {
        filesMap.remove(filePath)
        return rebuildGraph()
    }

    override fun getGraph(): RepositoryGraph {
        return currentGraph
    }

    override fun findImpactedComponents(changedFiles: List<String>): List<String> {
        val impacted = mutableSetOf<String>()
        changedFiles.forEach { file ->
            impacted.add(file)
            val fileAnalysis = filesMap[file]
            if (fileAnalysis != null) {
                fileAnalysis.symbols.forEach { sym ->
                    impacted.add(sym.qualifiedName)
                    // Find callers of this symbol
                    currentGraph.callersMap[sym.simpleName]?.let { callers ->
                        impacted.addAll(callers)
                    }
                    // Find subclasses
                    currentGraph.inheritanceMap[sym.simpleName]?.let { subclasses ->
                        impacted.addAll(subclasses)
                    }
                }
            }
        }
        return impacted.toList()
    }

    override fun findAssociatedTests(componentNameOrPath: String): List<String> {
        val simpleName = componentNameOrPath.substringAfterLast("/").removeSuffix(".kt").removeSuffix(".java")
        val tests = mutableSetOf<String>()

        filesMap.values.filter { it.isTestFile }.forEach { testFile ->
            val mentionsComponent = testFile.imports.any { it.contains(simpleName) } ||
                    testFile.symbols.any { it.simpleName.contains(simpleName) } ||
                    testFile.filePath.contains(simpleName)
            if (mentionsComponent) {
                tests.add(testFile.filePath)
            }
        }

        // Check test mapping
        currentGraph.testToFileMap[simpleName]?.let { mapped ->
            tests.addAll(mapped)
        }

        return tests.toList()
    }

    override fun findCallers(symbolName: String): List<String> {
        return currentGraph.callersMap[symbolName] ?: emptyList()
    }

    override fun findDependencies(symbolOrModule: String): List<String> {
        val module = modulesMap[symbolOrModule]
        if (module != null) return module.dependencies

        val symbol = currentGraph.symbols[symbolOrModule]
        if (symbol != null) {
            val file = filesMap[symbol.filePath]
            return file?.imports ?: emptyList()
        }
        return emptyList()
    }

    private fun scanModules(rootDir: File) {
        val settingsFile = File(rootDir, "settings.gradle.kts")
        if (settingsFile.exists()) {
            val content = settingsFile.readText()
            val includeRegex = Regex("""include\s*\(\s*["']([^"']+)["']\s*\)""")
            includeRegex.findAll(content).forEach { match ->
                val modPath = match.groupValues[1]
                val modName = modPath.removePrefix(":")
                val modDir = File(rootDir, modName.replace(":", "/"))
                val buildFile = File(modDir, "build.gradle.kts")
                val deps = mutableListOf<String>()
                val plugins = mutableListOf<String>()

                if (buildFile.exists()) {
                    val buildContent = buildFile.readText()
                    Regex("""implementation\s*\(([^)]+)\)""").findAll(buildContent).forEach { m ->
                        deps.add(m.groupValues[1].trim('"', '\''))
                    }
                    Regex("""alias\s*\(([^)]+)\)""").findAll(buildContent).forEach { m ->
                        plugins.add(m.groupValues[1].trim('"', '\''))
                    }
                }

                modulesMap[modName] = ModuleNode(
                    name = modName,
                    path = modDir.relativeToOrSelf(rootDir).path,
                    dependencies = deps,
                    plugins = plugins,
                    isAppModule = modName == "app"
                )
            }
        }
    }

    private fun detectCrossRepoLinks() {
        crossRepoLinks.add(
            CrossRepoLink(
                sourceRepo = "local/workspace",
                targetRepo = "mhvnsnt/M.-Engine-",
                relationship = "UPSTREAM_CANONICAL_SPECIFICATION",
                filePath = "AGENTS.md"
            )
        )
    }

    private fun parseFile(relativePath: String, content: String, lastModified: Long): FileAnalysis {
        var packageName = ""
        val imports = mutableListOf<String>()
        val symbols = mutableListOf<CodeSymbol>()
        val calls = mutableListOf<String>()
        val isTest = relativePath.contains("/test/") || relativePath.contains("/androidTest/") || relativePath.endsWith("Test.kt")

        val lines = content.lines()
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("package ")) {
                packageName = line.removePrefix("package ").removeSuffix(";").trim()
            } else if (line.startsWith("import ")) {
                val imp = line.removePrefix("import ").removeSuffix(";").trim()
                imports.add(imp)
            } else if (line.contains("class ") || line.contains("interface ") || line.contains("object ")) {
                val kind = when {
                    line.contains("interface ") -> SymbolKind.INTERFACE
                    line.contains("object ") -> SymbolKind.OBJECT
                    else -> SymbolKind.CLASS
                }
                val nameRegex = Regex("""(?:class|interface|object)\s+([A-Za-z0-9_]+)""")
                val match = nameRegex.find(line)
                if (match != null) {
                    val name = match.groupValues[1]
                    val superTypes = mutableListOf<String>()
                    val classHeader = line.substringBefore("{")
                    val superClause = if (classHeader.contains(")")) {
                        classHeader.substringAfterLast(")").substringAfter(":", "").trim()
                    } else {
                        classHeader.substringAfter(":", "").trim()
                    }
                    if (superClause.isNotEmpty()) {
                        superClause.split(",").map { it.trim().substringBefore("(").trim() }.filter { it.isNotEmpty() }.forEach {
                            superTypes.add(it)
                        }
                    }

                    symbols.add(
                        CodeSymbol(
                            qualifiedName = if (packageName.isNotEmpty()) "$packageName.$name" else name,
                            simpleName = name,
                            kind = kind,
                            filePath = relativePath,
                            packageName = packageName,
                            line = i + 1,
                            superTypes = superTypes
                        )
                    )
                }
            } else if (line.startsWith("fun ") || line.contains(" fun ")) {
                val funRegex = Regex("""fun\s+([A-Za-z0-9_]+)\s*\(""")
                val match = funRegex.find(line)
                if (match != null) {
                    val fnName = match.groupValues[1]
                    symbols.add(
                        CodeSymbol(
                            qualifiedName = if (packageName.isNotEmpty()) "$packageName.$fnName" else fnName,
                            simpleName = fnName,
                            kind = SymbolKind.FUNCTION,
                            filePath = relativePath,
                            packageName = packageName,
                            line = i + 1
                        )
                    )
                }
            }

            // Extract call patterns (e.g. SomeClass.method() or functionName())
            val callRegex = Regex("""([A-Za-z0-9_]+)\s*\(""")
            callRegex.findAll(line).forEach { cm ->
                val callTarget = cm.groupValues[1]
                if (callTarget !in listOf("if", "for", "while", "when", "switch", "catch", "synchronized", "fun")) {
                    calls.add(callTarget)
                }
            }
        }

        val hash = "${content.hashCode()}_${content.length}"
        return FileAnalysis(
            filePath = relativePath,
            packageName = packageName,
            imports = imports,
            symbols = symbols,
            functionCalls = calls.distinct(),
            isTestFile = isTest,
            hash = hash,
            lastModified = lastModified
        )
    }

    private fun rebuildGraph(): RepositoryGraph {
        val allSymbols = mutableMapOf<String, CodeSymbol>()
        val callers = mutableMapOf<String, MutableList<String>>()
        val inheritance = mutableMapOf<String, MutableList<String>>()
        val testMap = mutableMapOf<String, MutableList<String>>()

        filesMap.values.forEach { file ->
            file.symbols.forEach { sym ->
                allSymbols[sym.qualifiedName] = sym
                allSymbols[sym.simpleName] = sym

                sym.superTypes.forEach { superType ->
                    val cleanSuper = superType.substringBefore("<").trim()
                    inheritance.getOrPut(cleanSuper) { mutableListOf() }.add(sym.simpleName)
                }
            }

            file.functionCalls.forEach { call ->
                callers.getOrPut(call) { mutableListOf() }.add(file.filePath)
            }

            if (file.isTestFile) {
                file.imports.forEach { imp ->
                    val targetName = imp.substringAfterLast(".")
                    testMap.getOrPut(targetName) { mutableListOf() }.add(file.filePath)
                }
            }
        }

        currentGraph = RepositoryGraph(
            rootDir = rootDirectory.absolutePath,
            modules = modulesMap.toMap(),
            files = filesMap.toMap(),
            symbols = allSymbols,
            callersMap = callers.mapValues { it.value.distinct() },
            inheritanceMap = inheritance.mapValues { it.value.distinct() },
            testToFileMap = testMap.mapValues { it.value.distinct() },
            crossRepoLinks = crossRepoLinks.toList(),
            totalFilesIndexed = filesMap.size,
            indexedTimestamp = System.currentTimeMillis()
        )
        return currentGraph
    }
}
