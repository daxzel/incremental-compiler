package com.daxzel.compiler.compilation

import com.daxzel.compiler.db.BuildFile
import com.daxzel.compiler.db.BuildInfo
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import java.nio.file.Files
import java.nio.file.Path

class Compiler(val javac: JavacRunner) {

    fun compile(inputDir: Path, outputDir: Path) {
        Files.walk(inputDir)
            .filter { it.toFile().isFile }
            .forEach {
                javac.compileClass(it, inputDir, outputDir)
            }
    }

    fun compile(inputDir: Path, outputDir: Path, oldBuildInfo: BuildInfo?, newBuildInfo: BuildInfo) {
        Files.walk(inputDir)
            .filter { it.toFile().isFile }
            .forEach {

                val sourceHash = calcMD5HashForFile(it);
                val relativePath = inputDir.relativize(it)
                val outputClassFileName = javaToClassFilename(outputDir.resolve(relativePath))

                if (oldBuildInfo != null) {
                    // TODO: do we need to check if the file has been moved?
                    val buildFile = oldBuildInfo.files.filter {
                            it.sourceHash eq sourceHash
                        }.firstOrNull()
                    if (buildFile != null) {
                        // TODO: we need to check if the class still exists
                        val oldClassFileHash = calcMD5HashForFile(outputClassFileName)
                        if (buildFile.classHash == oldClassFileHash) {
                            return@forEach;
                        }
                    }
                }

                javac.compileClass(it, inputDir, outputDir)

                val buildValue = BuildFile.new {
                    this.sourceHash = calcMD5HashForFile(it)
                    this.classHash = calcMD5HashForFile(outputClassFileName)
                }

                newBuildInfo.files.add(buildValue)
            }
    }

}
