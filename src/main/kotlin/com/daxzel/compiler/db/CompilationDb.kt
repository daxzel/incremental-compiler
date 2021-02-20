package com.daxzel.compiler.db

import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdModel
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import kotlinx.dnq.xdRequiredStringProp
import java.io.Closeable
import java.nio.file.Path

class BuildInfo(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<BuildInfo>()

    var sourceFolder by xdRequiredStringProp(unique = true)
    var classpathFolder by xdRequiredStringProp(unique = true)

    var sourceFilesHash by xdRequiredStringProp()
    var classpathFilesHash by xdRequiredStringProp()
}

class CompilationDb(private val dir: Path) : Closeable {

    private val store: TransientEntityStore

    init {
        val userDir = System.getProperty("user.dir");
        println("Current working directory : $userDir");
        val dbPath = dir.resolve(".inc_compiler")

        XdModel.registerNodes(BuildInfo)

        store = StaticStoreContainer.init(
            dbFolder = dbPath.toFile(),
            environmentName = "compilation_db"
        )

        initMetaData(XdModel.hierarchy, store)
    }

    fun getLastBuildInfo(src: Path, classpath: Path): BuildInfo? {
        val srcStr = src.toString()
        val classpathStr = classpath.toString()

        return store.transactional {
            BuildInfo.all().filter {
                it.sourceFolder eq srcStr
            }.filter {
                it.classpathFolder eq classpathStr
            }.firstOrNull()
        }
    }

    fun storeBuildInfo(
        sourceFolder: Path, classpathFolder: Path, sourceFilesHash: String,
        classpathFilesHash: String
    ) {
        store.transactional {
            BuildInfo.new {
                this.sourceFolder = sourceFolder.toString()
                this.classpathFolder = classpathFolder.toString()
                this.sourceFilesHash = sourceFilesHash
                this.classpathFilesHash = classpathFilesHash
            }
        }
    }

    override fun close() {
        store.close()
    }
}
