package com.lightningkite.kotlinx.reflection.plugin

import java.io.File

fun reflectTask(lookForSources: List<File>, output: File, setupFunctionQualifiedName: String) {
    println("Finding files in ${lookForSources.joinToString()}...")

    val files = ArrayList<KxvFile>()
    //Search for files based on the ExternalReflection annotation
    lookForSources.forEach {
        it.walkTopDown().forEach {
            try {
                if (it.extension == "kt") {
                    val file = it.kotlinNode().getFile(it.name)
                    val filteredFile = file.copy(classes = file.classes.filter { it.annotations.any { it.name == "ExternalReflection" } })
                    if (filteredFile.classes.isNotEmpty()) {
                        files.add(filteredFile)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    println("Writing files into $output...")
    for (file in files) {
        try {
            val out = output.resolve(file.packageName.replace('.', File.separatorChar) + "/" + file.fileName)
            out.parentFile.mkdirs()
            out.writeText(file.write())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    println("Writing master file...")
    try {
        val masterFile = KxvSetupFunction(setupFunctionQualifiedName, files)
        val out = output.resolve(setupFunctionQualifiedName.replace('.', File.separatorChar) + ".kt")
        out.parentFile.mkdirs()
        out.writeText(masterFile.write())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    println("Complete.")
}