package com.daxzel.compiler

import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.EmptyVisitor
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.RemappingClassAdapter
import java.nio.file.Files
import java.nio.file.Path

private class ClassNameRecordingRemapper : Remapper() {

    val classNames: MutableSet<String> = mutableSetOf()

    override fun mapType(type: String): String {
        classNames.add(type)
        return type
    }
}

data class ClassInfo(val classname: String, val dependencies: Set<String>)

/**
 * Analise .class and provide information needed for incremental compilation.
 *
 */
fun getClassInfo(classFile: Path): ClassInfo {

    val bytecode: ByteArray = Files.readAllBytes(classFile)

    val classReader = ClassReader(bytecode)
    val remapper = ClassNameRecordingRemapper()

    val inner = EmptyVisitor()
    val visitor = RemappingClassAdapter(inner, remapper)
    classReader.accept(visitor, 0)

    return ClassInfo(classReader.className, remapper.classNames)
}