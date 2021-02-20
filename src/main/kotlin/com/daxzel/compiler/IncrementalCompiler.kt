package com.daxzel.compiler

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import com.daxzel.compiler.compilation.calcMD5HashForDir
import com.daxzel.compiler.db.getDb
import com.daxzel.compiler.db.getLastBuildInfo
import com.daxzel.compiler.db.storeBuildInfo
import java.nio.file.Path

class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(inputPath: Path, classpath: Path) {
        getDb(classpath).use { db ->

            val sourceFilesHash = calcMD5HashForDir(inputPath, setOf("java"))

            val oldClasspathFilesHash = calcMD5HashForDir(classpath, setOf("class"))

            db.transactional {

                val buildInfo = getLastBuildInfo()

                if (buildInfo != null) {
                    if (buildInfo.sourceFilesHash == sourceFilesHash &&
                        buildInfo.classpathFilesHash == oldClasspathFilesHash) {
                        return@transactional
                    }
                }

                val compiler = Compiler(javac)
                compiler.compile(inputPath, classpath)

                val newClasspathFilesHash = calcMD5HashForDir(classpath, setOf("class"))

                storeBuildInfo(inputPath, sourceFilesHash, newClasspathFilesHash)
            }
        }
    }
}

fun main(args: Array<String>) {
    TODO("Run compile")
}