package com.daxzel.compiler

import kotlinx.dnq.query.iterator
import java.nio.file.Files
import java.nio.file.Path

/**
 *  Information object which is used by compilation stages to understand input parameters of compilation.
 *  Logically it is read only information compared to [CompilationContext] which stores mutable information
 *  about compilation.
 */
class CompilationInfo(
    val inputDir: Path,
    val outputDir: Path,
    val javaFiles: Map<String, JavaToClass>,
    val previousBuildInfo: BuildInfo?,
    val javac: JavacRunner
)

/**
 * Storing mutable information between compilation stages. Some stages need to schedule dependant class for recompile.
 * // TODO: potentially we can implement it in immutable manner, think about it once compilation stages are finalized
 */
class CompilationContext {
    // Files which has been recompiled during current build
    val recompiled: MutableSet<JavaToClass> = mutableSetOf()

    // Files which should be recompiled regardless they have been changed or not.
    val recompileRequired: MutableSet<JavaToClass> = mutableSetOf()
}

/**
 * Remove classes for the java files which has been removed since the last build. Use [BuildInfo] to understand
 * which files have been used last time and if they are removed, remove classes as well.
 */
fun cleanClassesForRemovedFiles(info: CompilationInfo, context: CompilationContext) {
    // No previous compilation. We are not int position to clen any .class files from the target directory
    info.previousBuildInfo ?: return

    for (file in info.previousBuildInfo.files) {

        val existingJavaFile = info.javaFiles[file.relativePathStr]
        // The file is in info so it means it was there during the walking phase. We don't remove those.
        if (existingJavaFile != null) {
            continue
        }

        // We pretend that java class exists and create JavaToClass class.
        // TODO: JavaToClass concept doesn't really fit here very well, worth to think about alternatives
        val possibleJavaToClass = JavaToClass.get(info.inputDir, info.outputDir, file.relativePath)

        if (!possibleJavaToClass.javaFileAbsolute.toFile().exists()) {
            Files.deleteIfExists(possibleJavaToClass.classFileAbsolute)
            // In case some file has been removed we need to make sure all dependencies are scheduled for compilation
            // to detected build failure
            scheduleDependenciesForCompilation(possibleJavaToClass, info, context)
        }
    }
}

/**
 * Compile java files which are new or which has been changed. We use the last [BuildInfo] to understand
 * which files has been changed. If it is the first build we build all java files we have in [CompilationInfo]
 */
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
        internalCompilation(javaFile, info, context)
    }
}

private fun internalCompilation(
    javaFile: JavaToClass, info: CompilationInfo,
    context: CompilationContext
): JavacRunner.CompilationResult? {

    if (!context.recompiled.contains(javaFile)) {
        val result = info.javac.compileClass(javaFile.javaFileAbsolute, info.inputDir, info.outputDir)
        context.recompiled.add(javaFile)
        // We want dependencies to fail to compile in case the main one is failed so we delete previous .class file
        if (!result.successful) {
            Files.deleteIfExists(javaFile.classFileAbsolute)
        }
        return result
    }
    return null
}

/**
 * Build dependencies which require recompilation based on list stored in [CompilationContext].
 * This should be the late stage of compilation process after we gathered all dependencies which need recompilation
 * from other stages.
 */
fun compileDependencies(info: CompilationInfo, context: CompilationContext) {
    for (javaFile in context.recompileRequired) {
        val result = internalCompilation(javaFile, info, context)
        // if we failed to compile class it is possible that dependencies of this class also are not
        // compilable at the moment, we should try to recompile them to see
        if (result != null && !result.successful) {
            recursiveDependenciesCompilation(javaFile, info, context)
        }
    }
}

/**
 * Store information about the java files and their corresponding .class files into [newBuildInfo] so that we
 * can used this information next time we run compilation. We store information about the file we recompiled as well \
 * as the files which didn't need recompilation.
 */
@Suppress("UNUSED_PARAMETER") // just use info for consistency
fun fillUpNewBuildInfo(info: CompilationInfo, context: CompilationContext, newBuildInfo: BuildInfo) {
    val builtClasses: MutableMap<String, Pair<ClassInfo, BuildFileInfo>> = mutableMapOf()
    for (javaFile in context.recompiled) {

        val javaFileSHA1 = getSHA1(javaFile.javaFileAbsolute)
        val classFileSHA1 = getSHA1(javaFile.classFileAbsolute)

        // For some reasons we lost file after recompilation
        // TODO: handle concurrent changes during the build process
        classFileSHA1 ?: continue
        javaFileSHA1 ?: continue

        val newBuildFile = BuildFileInfo.new {
            this.sourceDirSHA1 = javaFileSHA1
            this.classDirSHA1 = classFileSHA1
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

/**
 * Run specified action on dependencies of a provided java file. To understand which files depend on the class we use
 * a previous compilation info. If the info is missing we are not going to run the action.
 */
private fun runOnDependencies(
    javaFile: JavaToClass, info: CompilationInfo,
    action: (dependency: JavaToClass) -> Unit
) {
    info.previousBuildInfo ?: return

    val buildFile = info.previousBuildInfo.getBuildFileInfo(javaFile.relativePath)
    buildFile ?: return

    for (dependsOnMe in buildFile.filesDependsOnMe) {
        val javaClassDependsOnMe = info.javaFiles[dependsOnMe.relativePathStr]
        if (javaClassDependsOnMe != null) {
            action(javaClassDependsOnMe)
        }
    }
}

private fun recursiveDependenciesCompilation(
    javaFile: JavaToClass, info: CompilationInfo,
    context: CompilationContext
) {
    runOnDependencies(javaFile, info) { dependency ->
        val result = internalCompilation(dependency, info, context)
        // if we failed to compile class it is possible that dependencies of this class also are not
        // compilable at the moment, we should try to recompile them to see.
        if (result != null && !result.successful) {
            recursiveDependenciesCompilation(dependency, info, context)
        }
    }
}

private fun scheduleDependenciesForCompilation(
    javaFile: JavaToClass, info: CompilationInfo,
    context: CompilationContext
) {
    runOnDependencies(javaFile, info) { dependency ->
        context.recompileRequired.add(dependency)
        // We don't schedule dependencies recursively as this decision is based on compilation result
        // so we do in latter stages of compilation when we starting compiling dependencies.
    }
}

private fun requiresRecompilation(javaFile: JavaToClass, buildFileInfo: BuildFileInfo): Boolean {

    val currentSourceHash = getSHA1(javaFile.javaFileAbsolute)
    currentSourceHash ?: return true // Source file is gone for some reasons

    val currentClassHash = getSHA1(javaFile.classFileAbsolute)
    currentClassHash ?: return true // Class file doesn't exists anymore so we need to rebuild

    if (buildFileInfo.sourceDirSHA1 != currentSourceHash) {
        return true // Source changed we for sure need recompilation
    }
    // safety check if somebody changed .class since last time we have been rebuilding it
    return buildFileInfo.classDirSHA1 != currentClassHash
}