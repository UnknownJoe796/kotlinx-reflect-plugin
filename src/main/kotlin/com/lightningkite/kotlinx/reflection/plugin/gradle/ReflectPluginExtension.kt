package com.lightningkite.kotlinx.reflection.plugin.gradle

import org.gradle.api.Project
import java.io.File

open class ReflectPluginExtension() {
    var lookForSources: List<File>? = null
    var output: File? = null
    var qualifiedSetupFunctionName: String = "com.lightningkite.kotlinx.reflection.setupGenerated"

    constructor(project: Project) : this() {
        lookForSources = arrayListOf(File(project.projectDir, "src/main/kotlin"), File(project.projectDir, "reflect/main/kotlin"))
        output = File(project.buildDir, "gen/reflect")
        qualifiedSetupFunctionName = "com.lightningkite.kotlinx.reflection.setupGeneratedFor${project.name.filter { it.isLetterOrDigit() }}"
    }
}