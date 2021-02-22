package com.daxzel.compiler

import com.daxzel.compiler.compilation.JavacRunner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

private const val NAME = "An incremental java compiler. Recompilation is done only for java files which has been " +
        "changed or depend on changed files."

class CompilerUtil : CliktCommand(name = NAME) {

    val sourceFilesDir: Path by option(help = "Directory with java files").path(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true
    ).required()

    val classFilesDir: Path by option(help = "Directory to store compiled java files ( .class files )").path(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true,
        mustBeWritable = true
    ).required()

    override fun run() {
        IncrementalCompiler(JavacRunner()).compile(sourceFilesDir, classFilesDir)
    }
}

fun main(args: Array<String>) = CompilerUtil().main(args)