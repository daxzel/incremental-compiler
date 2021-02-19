package com.daxzel.compiler

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import java.nio.file.Path


class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(inputPath: Path, classpath: Path, databasePath: Path) {
        val compiler = Compiler(javac)
        compiler.compile(inputPath, classpath)
    }

}

fun main(args: Array<String>) {
    TODO("Run compile")
}