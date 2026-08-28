package com.example.ai.mutation

import com.example.ai.capabilities.mutation.CodeMutationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeMutationEngineTest {

    @Test
    fun testFindDeclarationScope() {
        val source = """
            package com.test
            
            class Target {
                fun doWork() {
                    val a = "{ }" // tricky string
                    if (true) {
                        println("Hello")
                    }
                }
            }
            
            fun other() {}
        """.trimIndent()
        
        val engine = CodeMutationEngine()
        
        // Extract function
        val extractedFun = engine.extractDeclaration(source, "fun", "doWork")
        assertTrue(extractedFun.contains("tricky string"))
        assertTrue(extractedFun.contains("println(\"Hello\")"))
        assertTrue(extractedFun.endsWith("}"))
        
        // Extract class
        val extractedClass = engine.extractDeclaration(source, "class", "Target")
        assertTrue(extractedClass.startsWith("class Target"))
        assertTrue(extractedClass.endsWith("}"))
        assertTrue(extractedClass.contains("fun doWork"))
    }

    @Test
    fun testReplaceDeclaration() {
        val source = """
            fun oldMethod() {
                println("Old")
            }
        """.trimIndent()
        
        val engine = CodeMutationEngine()
        val newMethod = """
            fun oldMethod() {
                println("New")
            }
        """.trimIndent()
        
        val result = engine.replaceDeclaration(source, "fun", "oldMethod", newMethod)
        assertEquals(newMethod, result)
    }
}
