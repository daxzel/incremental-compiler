package com.daxzel.compiler.db

import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdModel
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import kotlinx.dnq.xdRequiredStringProp
import java.nio.file.Path

class BuildInfo(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<BuildInfo>()

    var sourceFilesHash by xdRequiredStringProp()
    var classpathFilesHash by xdRequiredStringProp()

    var sourceFolder by xdRequiredStringProp()
}

fun getDb(dir: Path): TransientEntityStore {
    val userDir = System.getProperty("user.dir");
    println("Current working directory : $userDir");
    val dbPath = dir.resolve(".inc_compiler")

    XdModel.registerNodes(BuildInfo)

    val store = StaticStoreContainer.init(
        dbFolder = dbPath.toFile(),
        environmentName = "compilation_db"
    )

    initMetaData(XdModel.hierarchy, store)
    return store
}

fun getLastBuildInfo(): BuildInfo? {
    return BuildInfo.all().firstOrNull()
}

fun storeBuildInfo(sourceFolder: Path, sourceFilesHash: String, classpathFilesHash: String) {
    val buildInfo = BuildInfo.findOrNew {}
    buildInfo.sourceFolder = sourceFolder.toString()
    buildInfo.sourceFilesHash = sourceFilesHash
    buildInfo.classpathFilesHash = classpathFilesHash
}
