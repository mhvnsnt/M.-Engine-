package com.example.ai

import android.util.Log

class TreeSitterEngine {
    // This provides structural semantics of the workspace code.
    // Instead of full JNI Tree-Sitter which requires heavy native libraries per language,
    // we use a lightweight Regex-based structural mapper to outline files for the agent.

    fun parseAST(code: String, language: String): String {
        Log.d("TreeSitter", "Parsing AST for language: $language")
        val lines = code.lines()
        
        return when (language.lowercase()) {
            "kotlin", "kt" -> parseKotlinJavaAST(lines, "Kotlin")
            "java" -> parseKotlinJavaAST(lines, "Java")
            "python", "py" -> parsePythonAST(lines)
            else -> "[AST Node: Root] contains ${lines.size} lines of $language code."
        }
    }

    private fun parseKotlinJavaAST(lines: List<String>, lang: String): String {
        val ast = StringBuilder()
        ast.append("[AST Root - $lang]\n")
        
        val classRegex = Regex("(?s).*(class|interface|object|enum class)\\s+([A-Za-z0-9_]+).*")
        val funRegex = Regex("(?s).*(fun|void|int|String|boolean|float|double|List|Map)\\s+([A-Za-z0-9_]+)\\s*\\(.*")
        val valVarRegex = Regex("(?s).*(val|var)\\s+([A-Za-z0-9_]+).*")
        
        var currentScope = ""
        
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            val trimmedLine = line.trim()
            
            if (trimmedLine.startsWith("import ") || trimmedLine.startsWith("package ")) {
                continue // Skip imports and packages for the outline
            }
            
            val classMatch = classRegex.matchEntire(trimmedLine)
            if (classMatch != null && !trimmedLine.startsWith("//")) {
                val type = classMatch.groupValues[1]
                val name = classMatch.groupValues[2]
                ast.append("L$lineNumber: [Declaration] $type $name\n")
                currentScope = name
                continue
            }
            
            val funMatch = funRegex.matchEntire(trimmedLine)
            if (funMatch != null && !trimmedLine.startsWith("//") && (trimmedLine.contains("fun ") || trimmedLine.contains("{") || trimmedLine.endsWith(")"))) {
                val name = funMatch.groupValues[2]
                if (name != "if" && name != "for" && name != "while" && name != "when" && name != "catch") {
                    ast.append("  L$lineNumber: [Function] $name(...)\n")
                }
                continue
            }
            
            val varMatch = valVarRegex.matchEntire(trimmedLine)
            if (varMatch != null && !trimmedLine.startsWith("//") && (trimmedLine.startsWith("val ") || trimmedLine.startsWith("var ") || trimmedLine.startsWith("private val "))) {
                val type = varMatch.groupValues[1]
                val name = varMatch.groupValues[2]
                ast.append("  L$lineNumber: [Property] $type $name\n")
            }
        }
        
        if (ast.toString().trim() == "[AST Root - $lang]") {
             ast.append("  (No structural elements found)\n")
        }
        
        return ast.toString()
    }

    private fun parsePythonAST(lines: List<String>): String {
        val ast = StringBuilder()
        ast.append("[AST Root - Python]\n")
        
        val classRegex = Regex("^\\s*class\\s+([A-Za-z0-9_]+).*")
        val defRegex = Regex("^\\s*def\\s+([A-Za-z0-9_]+).*")
        
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            val classMatch = classRegex.matchEntire(line)
            if (classMatch != null) {
                ast.append("L$lineNumber: [Class] ${classMatch.groupValues[1]}\n")
                continue
            }
            
            val defMatch = defRegex.matchEntire(line)
            if (defMatch != null) {
                val indent = line.takeWhile { it.isWhitespace() }.length
                val prefix = if (indent > 0) "  " else ""
                ast.append("$prefix L$lineNumber: [Function] ${defMatch.groupValues[1]}(...)\n")
            }
        }
        
        return ast.toString()
    }
}
