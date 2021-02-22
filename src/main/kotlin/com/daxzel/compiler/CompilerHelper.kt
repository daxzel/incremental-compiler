package com.daxzel.compiler

import kotlinx.dnq.query.iterator
import java.nio.file.Files
import java.nio.file.Path

class CompilationInfo(
    val inputDir: Path,
    val outputDir: Path,
    val javaFiles: Map<String, JavaToClass>, // Relative java class path -> JavaToClass
    val previousBuildInfo: BuildInfo?,
    val javac: JavacRunner
)

class CompilationContext {
    val recompiled: MutableSet<JavaToClass> = mutableSetOf()
    val recompileRequired: MutableSet<JavaToClass> = mutableSetOf()
}

fun cleanClassesForRemovedFiles(info: CompilationInfo, context: CompilationContext) {
    // No previous compilation. We are not int position to clen any .class files from the target directory
    info.previousBuildInfo ?: return

    for (file in info.previousBuildInfo.files) {

        val existingJavaFile = info.javaFiles[file.relativePathStr]
        // In the file is in info it means it was during the walking phase. We don't remove those
        if (existingJavaFile != null) {
            return
        }

        // We pretend that java class exists and create JavaToClass class.
        // TODO JavaToClass concept doesn't really fit here well, worth to think about alternatives
        val possibleJavaToClass = JavaToClass.get(info.inputDir, info.outputDir, file.relativePath)

        if (!possibleJavaToClass.javaFileAbsolute.toFile().exists()) {
            Files.deleteIfExists(possibleJavaToClass.classFileAbsolute)
            // In case some file has been removed we need to make sure all dependencies are scheduled for compilation
            // to detected build failure
            scheduleDependenciesForCompilation(possibleJavaToClass, info, context)
        }
    }
}

fun compileNewAndChanged(info: CompilationInfo, context: CompilationContext) {
    for (javaFile in info.javaFiles.values) {
        if (info.previousBuildInfo != null) {
            val buildFile = info.previousBuildInfo.getBuildFileInfo(javaFile.relativePath)
            if (buildFile != null) {
                if (!requiresRecompilation(javaFile, buildFile)) {
                    continue
                }
            }
        }
        scheduleDependenciesForCompilation(javaFile, info, context)
        info.javac.compileClass(javaFile.javaFileAbsolute, info.inputDir, info.outputDir)
        context.recompiled.add(javaFile)
    }
}

fun compileDependencies(info: CompilationInfo, context: CompilationContext) {
    for (javaFile in context.recompileRequired) {
        if (!context.recompiled.contains(javaFile)) {
            info.javac.compileClass(javaFile.javaFileAbsolute, info.inputDir, info.outputDir)
            context.recompiled.add(javaFile)
        }
    }
}


fun fillUpNewBuildInfo(info: CompilationInfo, context: CompilationContext, newBuildInfo: BuildInfo) {
    val builtClasses: MutableMap<String, Pair<ClassInfo, BuildFileInfo>> = mutableMapOf()
    for (javaFile in context.recompiled) {

        val javaFileHash = getMD5(javaFile.javaFileAbsolute)
        val classFileHash = getMD5(javaFile.classFileAbsolute)

        // For some reasons we lost file after recompilation
        // TODO handle failure in the build process
        classFileHash ?: continue
        javaFileHash ?: continue

        val newBuildFile = BuildFileInfo.new {
            this.sourceHash = javaFileHash
            this.classHash = classFileHash
            this.relativePathStr = javaFile.relativePath.toString()
        }

        newBuildInfo.files.add(newBuildFile)
        val classInfo = getClassInfo(javaFile.classFileAbsolute)
        builtClasses[classInfo.classname] = Pair(classInfo, newBuildFile)
    }

    for (builtClass in builtClasses) {
        val classInfo = builtClass.value.first
        for (dependency in classInfo.dependencies) {
            if (dependency != builtClass.key) { // TODO: find out why we have itself in dependencies
                val otherClass = builtClasses.get(dependency)
                otherClass?.second?.filesDependsOnMe?.add(builtClass.value.second)
            }
        }
    }
}

private fun scheduleDependenciesForCompilation(
    javaFile: JavaToClass, info: CompilationInfo,
    context: CompilationContext
) {
    info.previousBuildInfo ?: return

    val buildFile = info.previousBuildInfo.getBuildFileInfo(javaFile.relativePath)
    buildFile ?: return

    for (dependsOnMe in buildFile.filesDependsOnMe) {
        val javaClassDependsOnMe = info.javaFiles[dependsOnMe.relativePathStr]
        if (javaClassDependsOnMe != null) {
            if (context.recompileRequired.contains(javaClassDependsOnMe)) {
                context.recompileRequired.add(javaClassDependsOnMe)
                // TODO: We schedule recompilation of the whole even though we don't always need it - only if it
                //  fails we want dependencies to fail to compile as well
                scheduleDependenciesForCompilation(javaClassDependsOnMe, info, context)
            }
        }
    }
}

private fun requiresRecompilation(javaFile: JavaToClass, buildFileInfo: BuildFileInfo): Boolean {

    val currentSourceHash = getMD5(javaFile.javaFileAbsolute)
    currentSourceHash ?: return true // Source file is gone for some reasons

    val currentClassHash = getMD5(javaFile.classFileAbsolute)
    currentClassHash ?: return true // Class file doesn't exists anymore so we need to rebuild

    if (buildFileInfo.sourceHash != currentSourceHash) {
        return true // Source changed we for sure need recompilation
    }
    // safety check if somebody changed .class since last time we have been rebuilding it
    return buildFileInfo.classHash != currentClassHash
}