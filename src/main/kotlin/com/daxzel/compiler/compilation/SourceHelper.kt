package com.daxzel.compiler.compilation

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Path

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
