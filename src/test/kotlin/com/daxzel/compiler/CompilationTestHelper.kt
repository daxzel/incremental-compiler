package com.daxzel.compiler

import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

val USE_CASES_PATH = "/com/daxzel/compiler/usecases"

val ONE_CLASS_GROUP = "oneclass"
val TWO_CLASSES_GROUP = "twoclasses"
val THREE_CLASSES_GROUP = "threeclasses"

// Hack to make mockito work with kotlin, see
// https://stackoverflow.com/questions/30305217/is-it-possible-to-use-mockito-in-kotlin
private inline fun <reified T> any(): T = Mockito.any()

/**
 * Run incremental test on a test case. This method contains common logic of checking incremental compilation:
 * 1. Read before dir with .java files and use IncrementalCompiler to do the initial build
 * 2. Read after dir with changed .java file and use IncrementalCompiler to do build on the same target dir
 * 3. Do a non-incremental build and compare resulting .class files with step two
 *
 * @param group a type of the tests: oneclass, twoclasses, threeclasses, see com/daxzel/compiler/usecases dir
 * @param case a case number see com/daxzel/compiler/usecases/$group/case* dirs
 * @param beforeCompilations how many javac calls there should be on the initial build
 * @param afterCompilations how many javac calls there should be on the incremental build
 */
fun incrementalCompilationTest(group: String, case: Int, beforeCompilations: Int, afterCompilations: Int) {

    val javac = Mockito.spy(JavacRunner())
    val compiler = IncrementalCompiler(javac)

    val output = Files.createTempDirectory("compiler_output")
    val beforeInput = Paths.get(compiler.javaClass.getResource("$USE_CASES_PATH/$group/case$case/before").toURI())

    val afterInput = Paths.get(compiler.javaClass.getResource("$USE_CASES_PATH/$group/case$case/after").toURI())

    compiler.compile(beforeInput, output)

    verify(javac, times(beforeCompilations)).compileClass(any(), any(), any())
    Mockito.reset(javac)
    compiler.compile(afterInput, output)

    val testOutput = Files.createTempDirectory("compiler_output_javac")
    nonIncrementalCompile(afterInput, testOutput)

    assertTrue(compareClasses(output, testOutput))
    verify(javac, times(afterCompilations)).compileClass(any(), any(), any())
}

private fun nonIncrementalCompile(inputDir: Path, outputDir: Path) {
    val javac = JavacRunner()
    Files.walk(inputDir)
        .filter { it.toFile().isFile }
        .forEach {
            javac.compileClass(it, inputDir, outputDir)
        }
}
