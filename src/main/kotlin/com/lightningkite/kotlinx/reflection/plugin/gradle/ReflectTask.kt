package com.lightningkite.kotlinx.reflection.plugin.gradle

import com.lightningkite.kotlinx.reflection.plugin.reflectTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ReflectTask() : DefaultTask() {

    @Input
    var lookForSources = listOf(File(project.projectDir, "src/main/kotlin"), File(project.projectDir, "reflect/main/kotlin"))

    @Input
    var qualifiedSetupFunctionName: String = "com.lightningkite.kotlinx.reflection.setupGenerated"

    @OutputDirectory
    var output = File(project.buildDir, "gen/reflect")

    @TaskAction
    fun writeReflectiveFiles() {
        reflectTask(lookForSources, output, qualifiedSetupFunctionName)
    }

}