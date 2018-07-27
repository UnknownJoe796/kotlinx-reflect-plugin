# Kotlin External Reflection Plugin

!!! EXTREMELY WORK IN PROGRESS WARNING !!!

Reads your Kotlin source files and generates some reflective data about them.

Open the project and run the Gradle task "publishToMavenLocal" to add it to your local repositories.

Now, in the project you want to apply the plugin on, add the following:

```
buildscript {
    repositories {
        ...
        mavenLocal()
    }
    dependencies {
        ...
        classpath 'com.lightningkite.kotlinx:reflect-plugin:1.0.0'
    }
}

apply plugin: 'com.lightningkite.kotlinx.reflection'
```

Eventually, you will be able to configure it using the following in your gradle build file:

```
kxreflect {
    lookForSources = ["sources/found/here"]
    output = file("testOut")
    qualifiedSetupFunctionName = "com.ivieleague.setup"
}
```

For now, it looks inside your main source folder (src/main/kotlin) and one additional folder (reflect/main/kotlin) and puts the files into 'gen/reflect'.

Experiment away!