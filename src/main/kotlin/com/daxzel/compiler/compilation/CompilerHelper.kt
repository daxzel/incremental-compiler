package com.daxzel.compiler.compilation

import com.daxzel.compiler.db.BuildInfo
import kotlinx.dnq.query.iterator
import java.nio.file.Files
import java.nio.file.Path

class CompilationInfo(
    val inputDir: Path,
    val outputDir: Path,
    val javaFiles: Map<Path, JavaToClass>, // relative path -> JavaToClass
    val previousBuildInfo: BuildInfo?,
)

class CompilationContext(
    val javaClassesToRecompile: MutableSet<Path> = mutableSetOf(),
    val javaClassesRecompileRequired: MutableSet<JavaToClass> = mutableSetOf(),
)

fun cleanClassesBasedOnRemoved(info: CompilationInfo, context: CompilationContext) {
    info.previousBuildInfo ?: return

    for (file in info.previousBuildInfo.files) {

        val javaToClass = info.javaFiles[file.relativePath]
        if (javaToClass != null) {
            Files.deleteIfExists(javaToClass.javaFileAbsolute)
            // In case some file has been removed we need to make sure all dependencies are scheduled for compilation
            // to detected build failure
            for (dependsOnMe in file.filesDependsOnMe) {
                val javaToClassDepends = info.javaFiles[dependsOnMe.relativePath]
                // It is possible that dpenendency has been removed too so we need to check that it is still in the
                // source folder
                if (javaToClassDepends != null) {
                    context.javaClassesRecompileRequired.add(javaToClassDepends)
                }
            }
        }
    }
}
