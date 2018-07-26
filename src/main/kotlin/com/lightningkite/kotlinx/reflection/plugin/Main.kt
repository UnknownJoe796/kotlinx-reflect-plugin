package com.lightningkite.kotlinx.reflection.plugin

import java.io.File


fun main(vararg args: String) {
    val lookForSources = listOf(File("C:\\Users\\josep\\Projects\\reflect\\reflect-plugin\\testData"))
    val output = File("C:\\Users\\josep\\Projects\\reflect\\reflect-plugin\\build\\testOutput")
    val qualifiedSetupFunctionName = "com.lightningkite.kotlinx.reflection.setupGenerated"
    reflectTask(lookForSources, output, qualifiedSetupFunctionName)
}