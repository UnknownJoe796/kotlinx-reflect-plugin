package com.lightningkite.kotlinx.reflection.plugin

import org.gradle.internal.impldep.org.eclipse.jgit.lib.ObjectChecker.type


fun TabWriter.write(item: KxvDirectory) = with(item) {

    writeln("package ${qualifiedValName.substringBeforeLast('.')}")
    writeln()
    writeln("import com.lightningkite.kotlinx.reflection.kxReflect")
    writeln()
    for(it in classes){
        writeln("import ${it.packageName}.${it.reflectiveObjectName}")
    }
    writeln()
    writelnList(
            list = classes,
            prepend = "val ${qualifiedValName.substringAfterLast('.')} = listOf(",
            suffix = ")",
            howToWrite = { writeln(it.reflectiveObjectName) }
    )
    writeln()
}

fun TabWriter.write(item: KxvFile) = with(item) {
    writeln("package $packageName")
    writeln()
    writeln("import com.lightningkite.kotlinx.reflection.*")
    for (generalImport in generalImports) {
        writeln("import $generalImport.*")
    }
    for (specificImport in specificImports) {
        writeln("import $specificImport")
    }
    writeln()
    for (it in classes) {
        write(it)
        writeln()
    }
    writeln()
}

fun TabWriter.write(item: KxvClass) = with(item) {
    anyfy()
    val selfType = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection.STAR })
    val selfTypeAny = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection(KxvType("Any", true)) })

    writeln("object $reflectiveObjectName: KxClass<${selfType.emitStringActual()}> {")
    tabs++

    writeln("object Fields {")
    tabs++
    for (decl in variables.values) {
        writeln("val ${decl.name} by lazy { ")
        tabs++
        write(this, decl)
        tabs--
        writeln("}")
    }
    tabs--
    writeln("}")

    writeln("override val kclass get() = $simpleName::class")

    writeln("override val implements: List<KxType> by lazy {")
    tabs++
    writelnList(
            list = implements,
            prepend = "listOf<KxType>(",
            suffix = ")",
            howToWrite = {write(it)}
    )
    tabs--
    writeln("}")

    writeln("override val simpleName: String = \"$simpleName\"")
    writeln("override val qualifiedName: String = \"$packageName.$simpleName\"")

    writeln("override val values: Map<String, KxValue<${selfType.emitStringActual()}, *>> by lazy {")
    tabs++
    writeln(variables.values.filter { !it.mutable }.joinToString(", ", "mapOf<String, KxValue<${selfType.emitStringActual()}, *>>(", ")"){ """"${it.name}" to Fields.${it.name}""" })
    tabs--
    writeln("}")

    writeln("override val variables: Map<String, KxVariable<${selfType.emitStringActual()}, *>> by lazy {")
    tabs++
    writeln(variables.values.filter { it.mutable }.joinToString(", ", "mapOf<String, KxVariable<${selfType.emitStringActual()}, *>>(", ")"){ """"${it.name}" to Fields.${it.name}""" })
    tabs--
    writeln("}")

    writeln("override val functions: List<KxFunction<*>> by lazy {")
    tabs++
    writelnList(
            list = functions,
            prepend = "listOf<KxFunction<*>>(",
            suffix = ")",
            howToWrite = {write(it)}
    )
    tabs--
    writeln("}")

    writeln("override val constructors: List<KxFunction<${selfType.emitStringActual()}>> by lazy {")
    tabs++
    writelnList(
            list = constructors,
            prepend = "listOf<KxFunction<${selfType.emitStringActual()}>>(",
            suffix = ")",
            howToWrite = {write(it)}
    )
    tabs--
    writeln("}")

    writelnList(
            list = implements,
            prepend = "override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(",
            suffix = ")",
            howToWrite = {write(it)}
    )

    writeln("override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(${modifiers.joinToString { "KxClassModifier." + it.name }})")

    if (enumValues == null) {
        writeln("override val enumValues: List<${selfType.emitStringActual()}>? = null")
    } else {
        writelnList(
                list = enumValues!!,
                prepend = "override val enumValues: List<${selfType.emitStringActual()}>? = listOf<${selfType.emitStringActual()}>(",
                suffix = ")",
                howToWrite = {writeln("$simpleName.$it")}
        )
    }

    tabs--
    writeln("}")
}


fun TabWriter.write(item: KxvTypeProjection) {
    if (item.isStar) writeln("KxTypeProjection.STAR")
    else {
        writeln("KxTypeProjection(")
        tabs++
        writeln("type = ")
        tabs++
        write(item.type)
        tabs--
        writeln(",")
        writeln("variance = KxVariance.${item.variance.name}")
        tabs--
        writeln(")")
    }
}

fun TabWriter.write(item: KxvType) = with(item) {
    writeln("KxType(")
    tabs++

    writeln("base = $base::class.kxReflect,")
    writeln("nullable = $nullable,")

    writelnList(
            list = typeParameters,
            prepend = "typeParameters = listOf(",
            suffix = "),",
            howToWrite = {write(it)}
    )

    writelnList(
            list = annotations,
            prepend = "annotations = listOf(",
            suffix = ")",
            howToWrite = {write(it)}
    )

    tabs--
    writeln(")")
}

fun TabWriter.write(owner: KxvClass, item: KxvVariable) = with(item) {

    if (mutable) {
        writeln("KxVariable<${owner.selfType.emitStringActual()}, ${type.emitStringActual()}>(")
    } else {
        writeln("KxValue<${owner.selfType.emitStringActual()}, ${type.emitStringActual()}>(")
    }
    tabs++

    writeln("owner = ${owner.reflectiveObjectName},")
    writeln("""name = "$name",""")

    writeln("type = ")
    tabs++
    write(type)
    tabs--
    writeln(",")

    writeln("get = { owner -> owner.$name as ${type.emitStringActual()} },")

    if(mutable){
        writeln("set = " + if (owner.typeParameters.isNotEmpty()) {
            """{ owner, value -> (owner as ${owner.selfTypeAny.emitStringActual()}).$name = (value as ${type.emitStringActual()}) }"""
        } else {
            """{ owner, value -> owner.$name = value }"""
        } + ",")
    }

    writelnList(
            list = annotations,
            prepend = "annotations = listOf(",
            suffix = ")",
            howToWrite = {write(it)}
    )

    tabs--
    writeln(")")
}

fun TabWriter.write(arguments: List<KxvArgument>, item: KxvArgument) = with(item){
    val defaultText = default?.let {
        var current = it
        arguments.forEachIndexed { index, argument ->
            current = current.replace(Regex("\\b" + argument.name + "\\b"), "(previousArguments[$index] as ${argument.type.emitStringActual()})")
        }
        "{ previousArguments -> $current }"
    } ?: "null"

    writeln("KxArgument(")
    tabs++

    writeln("""name = "$name",""")

    writeln("type = ")
    tabs++
    write(type)
    tabs--
    append(",")

    writelnList(
            list = annotations,
            prepend = "annotations = listOf(",
            suffix = "),",
            howToWrite = {write(it)}
    )

    writeln("default = $defaultText")

    tabs--
    writeln(")")
}

fun TabWriter.write(item: KxvFunction) = with(item) {

    writeln("KxFunction<${type.emitStringActual()}>(")
    tabs++

    writeln("""name = "$name",""")

    writeln("type = ")
    tabs++
    write(type)
    tabs--
    writeln(",")


    writelnList(
            list = arguments,
            prepend = "arguments = listOf(",
            suffix = "),",
            howToWrite = {write(arguments, it)}
    )

    writeln("call = $callCode,")

    writelnList(
            list = annotations,
            prepend = "annotations = listOf(",
            suffix = ")",
            howToWrite = {write(it)}
    )

    tabs--
    writeln(")")
}

fun TabWriter.write(item: KxvAnnotation) = with(item){
    writeln("KxAnnotation(")
    tabs++
    writeln("""name = "$name",""")
    writeln("arguments = listOf(${arguments.joinToString()})")
    tabs--
    writeln(")")
}