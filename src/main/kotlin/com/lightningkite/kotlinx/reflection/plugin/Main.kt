package com.lightningkite.kotlinx.reflection.plugin

import java.io.File


fun main(vararg args: String) {
    val basePath = "/Users/josephivie/StudioProjects/kotlinx-reflect-plugin"
    val lookForSources = listOf(File("$basePath/testData"))
    val output = File("$basePath/build/testOutput")
    val qualifiedSetupFunctionName = "com.lightningkite.kotlinx.reflection.setupGenerated"

    lookForSources.forEach {
        it.walkTopDown().forEach {
            println(it)
            if(it.extension == "kt"){
                it.kotlinNode().print(System.out)
            }
        }
    }

    reflectTask(lookForSources, output, qualifiedSetupFunctionName)
}