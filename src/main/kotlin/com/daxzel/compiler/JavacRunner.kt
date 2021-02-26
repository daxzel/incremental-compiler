package com.daxzel.compiler;

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Single file compiler which calls javac. Mostly needed to be able to mock javac calls and calculator number of
 * javac calls in tests.
 */
class JavacRunner {

    data class CompilationResult(val successful: Boolean, val error: String? = null)

    fun compileClass(inputClass: Path, inputDir: Path, outputDir: Path): CompilationResult {
        val javacCommand = "javac -d $outputDir -cp $inputDir $inputClass"
        val process = Runtime.getRuntime().exec(javacCommand)
        return if (process.waitFor() != 0) {
            val error = IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)
            CompilationResult(false, error)
        } else {
            CompilationResult(true)
        }
    }

}
