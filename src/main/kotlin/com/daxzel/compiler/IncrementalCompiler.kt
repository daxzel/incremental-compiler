package com.daxzel.compiler

import com.daxzel.compiler.compilation.*
import com.daxzel.compiler.db.*
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.firstOrNull
import java.nio.file.Path
import java.util.stream.Collectors

class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(inputDir: Path, classpathDir: Path) {
        getDb(classpathDir).use { db ->

            db.transactional {

                val buildInfo = BuildInfo.all().firstOrNull()

                val javaClasses = walkJavaClasses(inputDir, classpathDir)
                    .collect(Collectors.toMap(JavaToClass::relativePath) { it });

                val info = CompilationInfo(inputDir, classpathDir, javaClasses, buildInfo, javac)
                val context = CompilationContext()

                cleanClassesBasedOnRemoved(info, context)
                compileNewAndChanged(info, context)
                compileDependencies(info, context)

                val newBuildInfo = BuildInfo.findOrNew {}
                newBuildInfo.sourceFolder = inputDir.toString()

                fillUpNewBuildInfo(info, context, newBuildInfo)
            }
        }
    }

}

fun main(args: Array<String>) {
    TODO("Run compile")
}