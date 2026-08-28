package com.example.ai.capabilities.mutation

class CodeMutationEngine {
    
    /**
     * Finds the start and end indices of a declaration (class, fun, interface)
     * by matching brackets, avoiding regex-based truncation errors.
     */
    fun findDeclarationScope(source: String, keyword: String, name: String): IntRange? {
        // Regex to find "fun name" or "class name"
        val pattern = Regex("""\b$keyword\s+$name\b""")
        val match = pattern.find(source) ?: return null
        
        val startIndex = match.range.first
        
        var openBraceIndex = -1
        for (i in match.range.last until source.length) {
            if (source[i] == '{') {
                openBraceIndex = i
                break
            }
            // If we hit a semicolon before a brace, it's not a block
            if (source[i] == ';') return null
        }
        
        if (openBraceIndex == -1) return null
        
        var braceCount = 1
        var endIndex = -1
        var inString = false
        var escape = false

        for (i in openBraceIndex + 1 until source.length) {
            val char = source[i]
            if (escape) {
                escape = false
                continue
            }
            if (char == '\\') {
                escape = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (char == '{') braceCount++
                else if (char == '}') braceCount--
                
                if (braceCount == 0) {
                    endIndex = i
                    break
                }
            }
        }
        
        if (endIndex != -1) {
            return startIndex..endIndex
        }
        return null
    }

    fun replaceDeclaration(source: String, keyword: String, name: String, newImplementation: String): String {
        val range = findDeclarationScope(source, keyword, name)
            ?: throw IllegalArgumentException("Declaration '$keyword $name' not found or unable to parse scope.")
            
        return source.replaceRange(range, newImplementation)
    }

    fun extractDeclaration(source: String, keyword: String, name: String): String {
        val range = findDeclarationScope(source, keyword, name)
            ?: throw IllegalArgumentException("Declaration '$keyword $name' not found or unable to parse scope.")
            
        return source.substring(range)
    }
}
