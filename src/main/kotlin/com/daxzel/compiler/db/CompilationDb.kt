package com.daxzel.compiler.db

import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.nio.file.Path

class BuildFile(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<BuildFile>()

    var sourceHash by xdRequiredStringProp()
    var classHash by xdRequiredStringProp()

    var buildInfo: BuildInfo by xdParent(BuildInfo::files)
}

class BuildInfo(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<BuildInfo>()

    var sourceDirHash by xdRequiredStringProp()
    var classpathDirHash by xdRequiredStringProp()

    var sourceFolder by xdRequiredStringProp()

    val files by xdChildren0_N(BuildFile::buildInfo)
}

fun getDb(dir: Path): TransientEntityStore {
    val userDir = System.getProperty("user.dir");
    println("Current working directory : $userDir");
    val dbPath = dir.resolve(".inc_compiler")

    XdModel.registerNodes(BuildInfo, BuildFile)

    val store = StaticStoreContainer.init(
        dbFolder = dbPath.toFile(),
        environmentName = "compilation_db" +
                ""
    )

    initMetaData(XdModel.hierarchy, store)
    return store
}

fun getLastBuildInfo(): BuildInfo? {
    return BuildInfo.all().firstOrNull()
}

fun storeBuildInfo(sourceFolder: Path, sourceDirHash: String, classpathDirHash: String) {
    val buildInfo = BuildInfo.findOrNew {}
    buildInfo.sourceFolder = sourceFolder.toString()
    buildInfo.sourceDirHash = sourceDirHash
    buildInfo.classpathDirHash = classpathDirHash
}
