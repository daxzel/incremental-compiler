package com.daxzel.compiler.compilation

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.*
import java.nio.file.Path
import java.util.*

fun File.calcMD5() = DigestUtils.md5Hex(FileUtils.readFileToByteArray(this))

fun compareTwoDirs(dir1: Path, dir2: Path): Boolean {
    val files1 = dir1.toFile().listFiles().sorted()
    val files2 = dir2.toFile().listFiles().sorted()
    if (files1.size != files2.size) return false
    return files1.zip(files2).all { equate(it.first, it.second) }
}

fun equate(fl: File, fl2: File): Boolean {
    if (fl.isFile && fl2.isFile) return fl.calcMD5() == fl2.calcMD5()
    if (fl.isDirectory && fl2.isDirectory) return compareTwoDirs(fl.toPath(), fl2.toPath())
    return false
}

fun calcMD5HashForDir(dirToHash: Path, extensions: Set<String>): String {
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
        val extension = FilenameUtils.getExtension(file.name);

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
