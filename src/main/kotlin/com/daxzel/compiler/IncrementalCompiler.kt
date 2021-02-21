package com.daxzel.compiler

import com.daxzel.compiler.compilation.Compiler
import com.daxzel.compiler.compilation.JavacRunner
import com.daxzel.compiler.compilation.getMD5Dir
import com.daxzel.compiler.db.BuildInfo
import com.daxzel.compiler.db.getDb
import com.daxzel.compiler.db.getLastBuildInfo
import com.daxzel.compiler.db.storeBuildInfo
import kotlinx.dnq.creator.findOrNew
import java.nio.file.Path

class IncrementalCompiler(val javac: JavacRunner) {

    fun compile(inputPath: Path, classpath: Path) {
        getDb(classpath).use { db ->

            val sourceDirHash = getMD5Dir(inputPath, setOf("java"))
            val classpathDirHash = getMD5Dir(classpath, setOf("class"))

            db.transactional {

                val buildInfo = getLastBuildInfo()

                if (buildInfo != null) {
                    if (buildInfo.sourceDirHash == sourceDirHash &&
                        buildInfo.classpathDirHash == classpathDirHash) {
                        return@transactional
                    }
                }

                val newBuildInfo = BuildInfo.findOrNew {}

                val compiler = Compiler(javac)
                compiler.compile(inputPath, classpath, buildInfo, newBuildInfo)

                val newClasspathDirHash = getMD5Dir(classpath, setOf("class"))
                newBuildInfo.sourceFolder = inputPath.toString()
                newBuildInfo.sourceDirHash = sourceDirHash
                newBuildInfo.classpathDirHash = newClasspathDirHash

                storeBuildInfo(inputPath, sourceDirHash, newClasspathDirHash)
            }
        }
    }
}

fun main(args: Array<String>) {
    TODO("Run compile")
}