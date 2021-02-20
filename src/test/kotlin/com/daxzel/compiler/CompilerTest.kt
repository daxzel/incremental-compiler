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

class CompilerTest {

    @Test
    fun testSimpleCompilation() {

        val output = Files.createTempDirectory("compiler_output_before")
        val input = Paths.get(javaClass.getResource("$USE_CASES_PATH/simple").toURI())

        IncrementalCompiler(JavacRunner()).compile(input, output)

        val javacOutput = Files.createTempDirectory("compiler_output_javac")
        Compiler(JavacRunner()).compile(input, javacOutput)

        assertTrue(compareTwoDirs(output, javacOutput))
    }

}
