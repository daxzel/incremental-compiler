package com.daxzel.compiler;

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
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
    fun testNoChangesCompilation() {

        val compilerDb = Files.createTempDirectory("compiler_db")

        val beforeOutput = Files.createTempDirectory("compiler_output_before")
        val beforeInput = Paths.get(javaClass.getResource("$USE_CASES_PATH/nochanges/before").toURI())

        val afterInput = Paths.get(javaClass.getResource("$USE_CASES_PATH/nochanges/after").toURI())
        val afterOutput = Files.createTempDirectory("compiler_output_after")

        compiler.compile(beforeInput, beforeOutput, compilerDb)

        verify(javac, atLeastOnce()).compileClass(any(), any())
        reset(javac)
        compiler.compile(afterInput, afterOutput, compilerDb)

        val testOutput = Files.createTempDirectory("compiler_output_javac")
        Compiler(JavacRunner()).compile(beforeInput, testOutput)

        assertTrue(compareTwoDirs(beforeOutput, afterInput))
        // make sure we haven't used javac the second time we called compilation
        verify(javac, never()).compileClass(any(), any())
    }

    fun File.calcMD5() = DigestUtils.md5Hex(FileUtils.readFileToByteArray(this))

    fun compareTwoDirs(dir1: Path, dir2: Path): Boolean {
        val files1 = dir1.toFile().listFiles().sorted()
        val files2 = dir2.toFile().listFiles().sorted()
        if (files1.size != files2.size) return false
        return files1.zip(files2).all { equate(it.first, it.second) }
    }

    fun equate(fl: File, fl2: File): Boolean {
        if (fl.isFile && fl2.isFile) return fl.calcMD5() == fl2.calcMD5()
        if (fl.isDirectory && fl2.isDirectory) return compareTwoDirs(fl.toPath(), fl2.toPath())
        return false
    }

}
