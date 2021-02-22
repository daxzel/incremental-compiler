package com.daxzel.compiler.db

import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.nio.file.Path
import java.nio.file.Paths

class BuildFileInfo(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<BuildFileInfo>()

    var relativePathStr by xdRequiredStringProp()

    var sourceHash by xdRequiredStringProp()
    var classHash by xdRequiredStringProp()

    var buildInfo: BuildInfo by xdParent(BuildInfo::files)

    val filesDependsOnMe by xdLink0_N(BuildFileInfo)

    val relativePath: Path
        get() = Paths.get(this.relativePathStr)
}

class BuildInfo(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<BuildInfo>()

    var sourceFolder by xdRequiredStringProp()

    val files by xdChildren0_N(BuildFileInfo::buildInfo)

    fun getBuildFileInfo(relativePath: Path): BuildFileInfo? {
        val relativePathString = relativePath.toString()
        return files.filter {
            it.relativePathStr eq relativePathString
        }.firstOrNull()
    }
}

fun getDb(dir: Path): TransientEntityStore {
    val userDir = System.getProperty("user.dir");
    println("Current working directory : $userDir");
    val dbPath = dir.resolve(".inc_compiler")

    XdModel.registerNodes(BuildInfo, BuildFileInfo)

    val store = StaticStoreContainer.init(
        dbFolder = dbPath.toFile(),
        environmentName = "compilation_db"
    )

    initMetaData(XdModel.hierarchy, store)
    return store
}