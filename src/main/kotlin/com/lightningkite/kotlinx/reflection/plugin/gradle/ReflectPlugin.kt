package com.lightningkite.kotlinx.reflection.plugin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ReflectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("kxreflect", ReflectPluginExtension::class.java)
        val task = target.tasks.create("kxreflect", ReflectTask::class.java)
        task.apply {
            extension.lookForSources?.let { this.lookForSources = it }
            extension.output?.let { this.output = it }
        }
        target.tasks.find { it.name == "build" }?.dependsOn(task)
    }
}

