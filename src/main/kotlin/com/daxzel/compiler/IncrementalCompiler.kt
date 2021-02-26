package com.daxzel.compiler

import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.firstOrNull
import java.nio.file.Path
import java.util.stream.Collectors

class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(sourceFilesDir: Path, classFilesDir: Path) {

        getDb(classFilesDir).use { db ->

            db.transactional {

                val buildInfo = BuildInfo.all().firstOrNull()

                val javaClasses = walkJavaClasses(sourceFilesDir, classFilesDir)
                    .collect(Collectors.toMap({ it.relativePath.toString() }, { it }))

                val info = CompilationInfo(sourceFilesDir, classFilesDir, javaClasses, buildInfo, javac)
                val context = CompilationContext()

                cleanClassesForRemovedFiles(info, context)
                compileNewAndChanged(info, context)
                compileDependencies(info, context)

                val newBuildInfo = BuildInfo.findOrNew {}
                newBuildInfo.sourceFolder = sourceFilesDir.toString()

                fillUpNewBuildInfo(info, context, newBuildInfo)
            }
        }
    }

}