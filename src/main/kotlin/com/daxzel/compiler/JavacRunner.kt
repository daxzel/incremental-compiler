package com.daxzel.compiler;

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class JavacError(message: String) : Exception(message)

class JavacRunner {

    fun compileClass(inputClass: Path, inputDir: Path, outputDir: Path) {
        val javacCommand = "javac -d $outputDir -cp $inputDir $inputClass"
        val process = Runtime.getRuntime().exec(javacCommand)
        if (process.waitFor() != 0) {
            val error = IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)
            throw JavacError("$javacCommand failed with $error")
        }
    }

}
