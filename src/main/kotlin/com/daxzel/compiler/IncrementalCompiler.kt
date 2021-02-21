package com.daxzel.compiler

import com.daxzel.compiler.compilation.*
import com.daxzel.compiler.compilation.dependencies.ClassInfo
import com.daxzel.compiler.compilation.dependencies.getClassInfo
import com.daxzel.compiler.db.*
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.iterator
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(inputDir: Path, classpathDir: Path) {
        getDb(classpathDir).use { db ->

            db.transactional {

                val buildInfo = BuildInfo.all().firstOrNull()

                val javaClasses = walkJavaClasses(inputDir, classpathDir)
                    .collect(Collectors.toMap(JavaToClass::relativePath) { it });

                val compilationInfo = CompilationInfo(inputDir, classpathDir, javaClasses, buildInfo)
                val compilationContext = CompilationContext()

                cleanClassesBasedOnRemoved(compilationInfo, compilationContext)

                val newBuildInfo = BuildInfo.findOrNew {}

                internalCompile(inputDir, classpathDir, buildInfo, newBuildInfo)

                newBuildInfo.sourceFolder = inputDir.toString()
            }
        }
    }

    private fun internalCompile(inputDir: Path, outputDir: Path, oldBuildInfo: BuildInfo?, newBuildInfo: BuildInfo) {

        val builtClasses: MutableMap<String, Pair<ClassInfo, BuildFileInfo>> = mutableMapOf()
        val builtInputFiles: MutableSet<Path> = mutableSetOf()
        val filesToBuild: MutableSet<Path> = mutableSetOf()

        Files.walk(inputDir)
            .filter { it.toFile().isFile }
            .forEach {

                val sourceHash = getMD5(it);
                val relativePath = inputDir.relativize(it).toString()
                val outputClassFileName = javaToClassFilename(outputDir.resolve(relativePath))

                var oldBuildFileInfo: BuildFileInfo? = null
                if (oldBuildInfo != null) {
                    // TODO: do we need to check if the file has been moved?
                    // TODO fill up build for the new build
                    oldBuildFileInfo = oldBuildInfo.files.filter {
                        it.relativePathStr eq relativePath
                    }.firstOrNull()
                }

                if (oldBuildFileInfo != null) {
                    // TODO: we need to check if the class still exists
                    val oldClassFileHash = getMD5(outputClassFileName)
                    if (oldBuildFileInfo.sourceHash == sourceHash && oldBuildFileInfo.classHash == oldClassFileHash) {
                        return@forEach;
                    }
                }

                if (oldBuildFileInfo != null) {
                    for (fileDependsOnMe in oldBuildFileInfo.filesDependsOnMe) {
                        filesToBuild.add(inputDir.resolve(fileDependsOnMe.relativePathStr))
                    }
                }

                builtInputFiles.add(it)

                javac.compileClass(it, inputDir, outputDir)

                val newBuildFile = BuildFileInfo.new {
                    this.sourceHash = getMD5(it)
                    this.classHash = getMD5(outputClassFileName)
                    this.relativePathStr = relativePath
                }

                newBuildInfo.files.add(newBuildFile)

                val classInfo = getClassInfo(outputClassFileName)

                builtClasses.put(classInfo.classname, Pair(classInfo, newBuildFile))
            }

        for (toBuild in filesToBuild) {
            if (!builtInputFiles.contains(toBuild)) {

                //TODO copy past from before
                val relativePath = inputDir.relativize(toBuild)
                val outputClassFileName = javaToClassFilename(outputDir.resolve(relativePath))

                javac.compileClass(toBuild, inputDir, outputDir)

                val newBuildFile = BuildFileInfo.new {
                    this.sourceHash = getMD5(toBuild)
                    this.classHash = getMD5(outputClassFileName)
                    this.relativePathStr = relativePath.toString()
                }

                newBuildInfo.files.add(newBuildFile)

                val classInfo = getClassInfo(outputClassFileName)

                builtClasses.put(classInfo.classname, Pair(classInfo, newBuildFile))
            }

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

}

fun main(args: Array<String>) {
    TODO("Run compile")
}