package com.daxzel.compiler

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import com.daxzel.compiler.compilation.calcMD5HashForDir
import com.daxzel.compiler.db.CompilationDb
import java.nio.file.Path


class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(inputPath: Path, classpath: Path) {
        CompilationDb().use { db ->
            val compiler = Compiler(javac)
            compiler.compile(inputPath, classpath)

            val sourceFilesHash = calcMD5HashForDir(inputPath, setOf("java"))
            val classpathFilesHash = calcMD5HashForDir(inputPath, setOf("class"))

            db.storeBuildInfo(inputPath, classpath, sourceFilesHash, classpathFilesHash)
        }
    }
}

fun main(args: Array<String>) {
    TODO("Run compile")
}