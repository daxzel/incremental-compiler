package com.daxzel.compiler.compilation

import com.daxzel.compiler.compilation.dependencies.ClassInfo
import com.daxzel.compiler.compilation.dependencies.getClassInfo
import com.daxzel.compiler.db.BuildFile
import com.daxzel.compiler.db.BuildInfo
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.iterator
import java.nio.file.Files
import java.nio.file.Path

class Compiler(val javac: JavacRunner) {

    fun compile(inputDir: Path, outputDir: Path) {
        Files.walk(inputDir)
            .filter { it.toFile().isFile }
            .forEach {
                javac.compileClass(it, inputDir, outputDir)
            }
    }

    fun compile(inputDir: Path, outputDir: Path, oldBuildInfo: BuildInfo?, newBuildInfo: BuildInfo) {

        val builtClasses: MutableMap<String, Pair<ClassInfo, BuildFile>> = mutableMapOf()
        val builtInputFiles: MutableSet<Path> = mutableSetOf()
        val filesToBuild: MutableSet<Path> = mutableSetOf()

        Files.walk(inputDir)
            .filter { it.toFile().isFile }
            .forEach {

                val sourceHash = getMD5(it);
                val relativePath = inputDir.relativize(it).toString()
                val outputClassFileName = javaToClassFilename(outputDir.resolve(relativePath))

                var oldBuildFile: BuildFile? = null
                if (oldBuildInfo != null) {
                    // TODO: do we need to check if the file has been moved?
                    // TODO fill up build for the new build
                    oldBuildFile = oldBuildInfo.files.filter {
                        it.relativePath eq relativePath
                    }.firstOrNull()
                }

                if (oldBuildFile != null) {
                    // TODO: we need to check if the class still exists
                    val oldClassFileHash = getMD5(outputClassFileName)
                    if (oldBuildFile.sourceHash == sourceHash && oldBuildFile.classHash == oldClassFileHash) {
                        return@forEach;
                    }
                }

                if (oldBuildFile != null) {
                    for (fileDependsOnMe in oldBuildFile.filesDependsOnMe) {
                        filesToBuild.add(inputDir.resolve(fileDependsOnMe.relativePath))
                    }
                }

                builtInputFiles.add(it)

                javac.compileClass(it, inputDir, outputDir)

                val newBuildFile = BuildFile.new {
                    this.sourceHash = getMD5(it)
                    this.classHash = getMD5(outputClassFileName)
                    this.relativePath = relativePath
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

                val newBuildFile = BuildFile.new {
                    this.sourceHash = getMD5(toBuild)
                    this.classHash = getMD5(outputClassFileName)
                    this.relativePath = relativePath.toString()
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
