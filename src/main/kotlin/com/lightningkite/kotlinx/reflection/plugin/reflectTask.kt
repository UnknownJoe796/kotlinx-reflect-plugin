package com.lightningkite.kotlinx.reflection.plugin

import java.io.File

fun reflectTask(lookForSources: List<File>, output: File, setupFunctionQualifiedName: String) {
    println("Finding files in ${lookForSources.joinToString()}...")

    val files = ArrayList<KxvFile>()
    //Search for files based on the ExternalReflection annotation
    lookForSources.forEach {
        it.walkTopDown().forEach cont@{
            try {
                if (it.extension != "kt") return@cont
                if (!it.readText().contains("ExternalReflection")) return@cont
                val file = it.kotlinNode().getFile(it.name)
                val filteredFile = file.copy(classes = file.classes.filter { it.annotations.any { it.name == "ExternalReflection" } })
                if (filteredFile.classes.isNotEmpty()) {
                    files.add(filteredFile)
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
            out.bufferedWriter().use {
                val tabWriter = TabWriter(it)
                tabWriter.write(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    println("Writing master files...")
    try {
        files
                .flatMap { it.classes }
                .groupBy { it.packageName }
                .map { KxvDirectory(it.key + ".reflections", it.value) }
                .forEach { kxvDirectory ->
                    val out = output.resolve(kxvDirectory.qualifiedValName.replace('.', File.separatorChar) + ".kt")
                    out.parentFile.mkdirs()
                    out.bufferedWriter().use {
                        val tabWriter = TabWriter(it)
                        tabWriter.write(kxvDirectory)
                    }
                }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    println("Complete.")
}