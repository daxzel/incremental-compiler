package com.daxzel.compiler

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import com.daxzel.compiler.compilation.compareTwoDirs
import org.junit.jupiter.api.Assertions
import org.mockito.Mockito
import org.mockito.Mockito.times
import java.nio.file.Files
import java.nio.file.Paths

val USE_CASES_PATH = "/com/daxzel/compiler/usecases"

val ONE_CLASS_GROUP = "oneclass"
val TWO_CLASSES_GROUP = "twoclasses"

private inline fun <reified T> any(): T = Mockito.any()

fun incrementalCompilationTest(group: String, case: Int, beforeCompilations: Int, afterCompilations: Int) {

    val javac = Mockito.spy(JavacRunner())
    val compiler = IncrementalCompiler(javac)

    val beforeOutput = Files.createTempDirectory("compiler_output_before")
    val beforeInput = Paths.get(compiler.javaClass.getResource("$USE_CASES_PATH/$group/case$case/before").toURI())

    val afterInput = Paths.get(compiler.javaClass.getResource("$USE_CASES_PATH/$group/case$case/after").toURI())
    val afterOutput = Files.createTempDirectory("compiler_output_after")

    compiler.compile(beforeInput, beforeOutput)

    Mockito.verify(javac, times(beforeCompilations)).compileClass(any(), any())
    Mockito.reset(javac)
    compiler.compile(afterInput, afterOutput)

    val testOutput = Files.createTempDirectory("compiler_output_javac")
    Compiler(JavacRunner()).compile(beforeInput, testOutput)

    Assertions.assertTrue(compareTwoDirs(beforeOutput, afterInput))
    Mockito.verify(javac, times(afterCompilations)).compileClass(any(), any())
}