package com.daxzel.compiler.compilation

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

class JavaToClass(val relativePath: Path, val javaFileAbsolute: Path, val classFileAbsolute: Path)

fun walkJavaClasses(inputDir: Path, outputDir: Path): Stream<JavaToClass> {
    return Files.walk(inputDir)
        .filter { it.toFile().isFile }
        .filter { getExtension(it.toString()) == JAVA_EXTENSION }
        .map {
            val relativePath = inputDir.relativize(it)
            val outputClassFileName = javaToClassFilename(outputDir.resolve(relativePath))
            JavaToClass(relativePath, it, outputClassFileName)
        }
}

fun javaToClassFilename(javaFileName: Path): Path {
    val fileName = javaFileName.toAbsolutePath().toString();
    assert(getExtension(fileName) == JAVA_EXTENSION)
    val filenameWithoutExtension = FilenameUtils.removeExtension(fileName)
    return Paths.get("$filenameWithoutExtension.$CLASS_EXTENSION")
}

fun compareClasses(dir1: Path, dir2: Path): Boolean {
    val extensions = setOf(CLASS_EXTENSION)
    return getMD5Dir(dir1, extensions) == getMD5Dir(dir2, extensions)
}

fun getMD5(file: Path): String {
    Files.newInputStream(file).use {
        return DigestUtils.md5Hex(it);
    }
}

fun getMD5Dir(dirToHash: Path, extensions: Set<String>): String {
    assert(dirToHash.toFile().isDirectory)
    val fileStreams = mutableListOf<FileInputStream>()
    collectInputStreams(dirToHash, fileStreams, extensions)
    val seqStream = SequenceInputStream(Collections.enumeration(fileStreams))
    try {
        val md5Hash = DigestUtils.md5Hex(seqStream)
        seqStream.close()
        return md5Hash
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
