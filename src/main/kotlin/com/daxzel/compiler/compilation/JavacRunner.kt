package com.daxzel.compiler.compilation;

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class JavacError(message: String) : Exception(message)

class JavacRunner {

    fun compileClass(inputClass: Path, outputDir: Path) {
        val javacCommand = "javac -d $outputDir $inputClass"
        val process = Runtime.getRuntime().exec(javacCommand)
        if (process.waitFor() != 0) {
            val error = IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)
            throw JavacError("$javacCommand failed with $error")
        }
    }

}
