package com.daxzel.compiler.compilation

import java.nio.file.Files
import java.nio.file.Path

class Compiler(val javac: JavacRunner) {

    /**
     * Run javac on all files in input directory and store resulting .class into output directory.
     */
    fun compile(inputDir: Path, outputDir: Path) {
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
            javac.compileClass(src, dst)
        }
    }

}
