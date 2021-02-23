package com.daxzel.compiler

import kotlinx.dnq.query.iterator
import java.nio.file.Files
import java.nio.file.Path

/**
 *  Information object which is used by compilation stages to understand input parameters of compilation.
 *  Logically it is read only information compared to [CompilationContext] which stores mutable information
 *  about compilation.
 *
 * @property inputDir dir with java files
 * @property outputDir dir with class files
 * @property javaFiles list of java files we are compiling, keys are relative path to jave files
 *      like [inputDir]/relativePath
 * @property previousBuildInfo information about previous build, could be null if we start for the first time
 * @property javac runner for javac
 */
class CompilationInfo(
    val inputDir: Path,
    val outputDir: Path,
    val javaFiles: Map<String, JavaToClass>,
    val previousBuildInfo: BuildInfo?,
    val javac: JavacRunner
)

/**
 * Store mutable information between compilation stages. Some stages need to schedule dependant class for recompile.
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
 *
 * @param info see [CompilationInfo]
 * @param context see [CompilationContext]
 */
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
 *
 * @param info see [CompilationInfo]
 * @param context see [CompilationContext]
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
        info.javac.compileClass(javaFile.javaFileAbsolute, info.inputDir, info.outputDir)
        context.recompiled.add(javaFile)
    }
}

/**
 * Build dependencies which require recompilation based on list stored in [CompilationContext].
 * This should be the late stage of compilation process after we gathered all dependencies which need recompilation
 * from other stages.
 *
 * @param info see [CompilationInfo]
 * @param context see [CompilationContext]
 */
fun compileDependencies(info: CompilationInfo, context: CompilationContext) {
    for (javaFile in context.recompileRequired) {
        if (!context.recompiled.contains(javaFile)) {
            info.javac.compileClass(javaFile.javaFileAbsolute, info.inputDir, info.outputDir)
            context.recompiled.add(javaFile)
        }
    }
}

/**
 * Store information about the java files and their corresponding .class files into [newBuildInfo] so that we
 * can used this information next time we run compilation. We store information about the file we recompiled as well \
 * as the files which didn't need recompilation.
 *
 * @param info see [CompilationInfo]
 * @param context see [CompilationContext]
 * @param newBuildInfo current compilation [BuildInfo], being updated in this function
 */
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