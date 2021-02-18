package com.daxzel.compiler;

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CompilerTest {

    private val USE_CASES_PATH = "/com/daxzel/compiler/usecases"

    @Test
    fun testSimpleCompilation() {

        val compilerDb = Files.createTempDirectory("compiler_db")

        val output = Files.createTempDirectory("compiler_output_before")
        val input = Paths.get(javaClass.getResource("$USE_CASES_PATH/simple").toURI())

        compile(input, output, compilerDb)

        val javacOutput = Files.createTempDirectory("compiler_output_javac")
        runJavaC(input, javacOutput)

        assertTrue(compareTwoDirs(output, javacOutput))
    }

    @Test
    fun testNoChangesCompilation() {

        val compilerDb = Files.createTempDirectory("compiler_db")

        val beforeOutput = Files.createTempDirectory("compiler_output_before")
        val beforeInput = Paths.get(javaClass.getResource("$USE_CASES_PATH/nochanges/before").toURI())

        val afterInput = Paths.get(javaClass.getResource("$USE_CASES_PATH/nochanges/after").toURI())
        val afterOutput = Files.createTempDirectory("compiler_output_after")

        compile(beforeInput, beforeOutput, compilerDb)
        compile(afterInput, afterOutput, compilerDb)

        val testOutput = Files.createTempDirectory("compiler_output_javac")
        runJavaC(beforeInput, testOutput)

        assertTrue(compareTwoDirs(beforeOutput, afterInput))
    }

    fun runJavaC(inputDir: Path, outputDir: Path) {
        val srcClassToOutput = Files.walk(inputDir)
            .filter { it.toFile().isFile }
            .map {
            // To get the output java class absolute path we first need to fine relative path for input
            val outputClassPath = outputDir.resolve(inputDir.relativize(it))
            // For output javac gets a dir.
            val outputClassDirPath = outputClassPath.parent
            // e.g. /inputDir/package/main.java -> /outputDir/package
            Pair(it, outputClassDirPath)
        }

        srcClassToOutput.forEach { (src, dst) ->
            Files.createDirectories(dst)
            val javacCommand = "javac -d $dst $src"
            val process = Runtime.getRuntime().exec(javacCommand)
            assertEquals(0, process.waitFor()) {
                val error = IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)
                "$javacCommand failed with $error"
            }
        }
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
