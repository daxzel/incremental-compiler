package com.daxzel.compiler

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import com.daxzel.compiler.compilation.compareClasses
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.nio.file.Files
import java.nio.file.Paths

val USE_CASES_PATH = "/com/daxzel/compiler/usecases"

val ONE_CLASS_GROUP = "oneclass"
val TWO_CLASSES_GROUP = "twoclasses"

private inline fun <reified T> any(): T = Mockito.any()

fun incrementalCompilationTest(group: String, case: Int, beforeCompilations: Int, afterCompilations: Int) {

    val javac = Mockito.spy(JavacRunner())
    val compiler = IncrementalCompiler(javac)

    val output = Files.createTempDirectory("compiler_output")
    val beforeInput = Paths.get(compiler.javaClass.getResource("$USE_CASES_PATH/$group/case$case/before").toURI())

    val afterInput = Paths.get(compiler.javaClass.getResource("$USE_CASES_PATH/$group/case$case/after").toURI())

    compiler.compile(beforeInput, output)

    verify(javac, times(beforeCompilations)).compileClass(any(), any())
    Mockito.reset(javac)
    compiler.compile(afterInput, output)

    val testOutput = Files.createTempDirectory("compiler_output_javac")
    Compiler(JavacRunner()).compile(beforeInput, testOutput)

    assertTrue(compareClasses(output, testOutput))
    verify(javac, times(afterCompilations)).compileClass(any(), any())
}
