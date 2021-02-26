package com.daxzel.compiler

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.FilenameUtils.getExtension
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream

private const val CLASS_EXTENSION = "class"
private const val JAVA_EXTENSION = "java"

/**
 * Represents a single build target, in out case it is a java file. It has a relative path, an absolution path and path
 * to potential .class file. TODO: Handle multiple .class files when with inner classes
 */
class JavaToClass private constructor(val relativePath: Path,
                                      val javaFileAbsolute: Path, val classFileAbsolute: Path) {
    companion object {
        fun get(inputDir: Path, outputDir: Path, relativeJavaFile: Path): JavaToClass {
            val outputClassFileName = javaToClassFilename(outputDir.resolve(relativeJavaFile))
            return JavaToClass(relativeJavaFile, inputDir.resolve(relativeJavaFile), outputClassFileName)
        }
    }
}

/**
 * Traverse [inputDir] and file all .java class and create a stream [JavaToClass] instances.
 */
fun walkJavaClasses(inputDir: Path, outputDir: Path): Stream<JavaToClass> {
    return Files.walk(inputDir)
        .filter { it.toFile().isFile }
        .filter { getExtension(it.toString()) == JAVA_EXTENSION }
        .map {
            val relativePath = inputDir.relativize(it)
            JavaToClass.get(inputDir, outputDir, relativePath)
        }
}
/**
 * Based on a .java file get a .class file name.
 */
fun javaToClassFilename(javaFileName: Path): Path {
    val fileName = javaFileName.toAbsolutePath().toString();
    assert(getExtension(fileName) == JAVA_EXTENSION)
    val filenameWithoutExtension = FilenameUtils.removeExtension(fileName)
    return Paths.get("$filenameWithoutExtension.$CLASS_EXTENSION")
}

/**
 * Compare .class files in two directors
 * @return true if directories are identical from perspective of .class files.
 */
fun compareClasses(dir1: Path, dir2: Path): Boolean {
    val extensions = setOf(CLASS_EXTENSION)
    return getSHA1Dir(dir1, extensions) == getSHA1Dir(dir2, extensions)
}

fun getSHA1(file: Path): String? {
    if (!file.toFile().exists()) {
        return null
    }
    assert(file.toFile().isFile)
    Files.newInputStream(file).use {
        return DigestUtils.sha1Hex(it);
    }
}

fun getSHA1Dir(dirToHash: Path, extensions: Set<String>): String {
    assert(dirToHash.toFile().isDirectory)
    val fileStreams = mutableListOf<FileInputStream>()
    collectInputStreams(dirToHash, fileStreams, extensions)
    val seqStream = SequenceInputStream(Collections.enumeration(fileStreams))
    try {
        val sha1Hash = DigestUtils.sha1Hex(seqStream)
        seqStream.close()
        return sha1Hash
    } catch (e: IOException) {
        throw RuntimeException(
            "Error reading files to hash in " + dirToHash.toAbsolutePath().toString(), e
        )
    }
}

private fun collectInputStreams(dir: Path, foundStreams: MutableList<FileInputStream>, extensions: Set<String>) {
    val fileList = dir.toFile().listFiles()
    Arrays.sort(fileList) { f1, f2 -> f1.name.compareTo(f2.name) }
    for (file: File in fileList!!) {
        val extension = getExtension(file.name)
        when {
            file.isDirectory -> collectInputStreams(file.toPath(), foundStreams, extensions)
            extensions.contains(extension) -> {
                try {
                    foundStreams.add(FileInputStream(file))
                } catch (e: FileNotFoundException) {
                    throw AssertionError(
                        (e.message + ": file should never not be found!")
                    )
                }
            }
        }
    }
}
