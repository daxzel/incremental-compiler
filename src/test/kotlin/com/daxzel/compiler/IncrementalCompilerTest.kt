package com.daxzel.compiler;

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import com.daxzel.compiler.compilation.compareTwoDirs
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.nio.file.Files
import java.nio.file.Paths

private inline fun <reified T> any(): T = Mockito.any()

class IncrementalCompilerTest {

    private val USE_CASES_PATH = "/com/daxzel/compiler/usecases"

    private val compiler: IncrementalCompiler
    private val javac: JavacRunner

    init {
        javac = spy(JavacRunner())
        compiler = IncrementalCompiler(javac)
    }

    @BeforeEach
    fun cleanSpy() {
        reset(javac)
    }

    @Test
    fun testSimpleCompilation() {

        val compilerDb = Files.createTempDirectory("compiler_db")

        val output = Files.createTempDirectory("compiler_output_before")
        val input = Paths.get(javaClass.getResource("$USE_CASES_PATH/simple").toURI())

        compiler.compile(input, output, compilerDb)

        val javacOutput = Files.createTempDirectory("compiler_output_javac")
        Compiler(JavacRunner()).compile(input, javacOutput)

        assertTrue(compareTwoDirs(output, javacOutput))
    }

    @Test
    fun testCase1() {
        // We have only one class. No changes in the file so theoretically
        // we shouldn't call compilation a second time.
        incrementalCompilationTest("case1", 1, 0)
    }

    @Test
    fun testCase2() {
        // We have only one class. The method in the class changed so we should call compilation in the second pass.
        incrementalCompilationTest("case2", 1, 1)
    }

    @Test
    fun testCase3() {
        // We have only one class. A new method added to the class so we should call compilation in the second pass.
        incrementalCompilationTest("case3", 1, 1)
    }

    @Test
    fun testCase4() {
        // We have only one class. A new method added to the class so we should call compilation in the second pass.
        incrementalCompilationTest("case4", 2, 1)
    }

    private fun incrementalCompilationTest(testCase: String, beforeCompilations: Int, afterCompilations: Int) {

        val compilerDb = Files.createTempDirectory("compiler_db")

        val beforeOutput = Files.createTempDirectory("compiler_output_before")
        val beforeInput = Paths.get(javaClass.getResource("$USE_CASES_PATH/$testCase/before").toURI())

        val afterInput = Paths.get(javaClass.getResource("$USE_CASES_PATH/$testCase/after").toURI())
        val afterOutput = Files.createTempDirectory("compiler_output_after")

        compiler.compile(beforeInput, beforeOutput, compilerDb)

        verify(javac, times(beforeCompilations)).compileClass(any(), any())
        reset(javac)
        compiler.compile(afterInput, afterOutput, compilerDb)

        val testOutput = Files.createTempDirectory("compiler_output_javac")
        Compiler(JavacRunner()).compile(beforeInput, testOutput)

        assertTrue(compareTwoDirs(beforeOutput, afterInput))
        verify(javac, times(afterCompilations)).compileClass(any(), any())
    }

}
