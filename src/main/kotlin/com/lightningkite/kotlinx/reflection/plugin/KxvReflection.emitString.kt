package com.lightningkite.kotlinx.reflection.plugin


fun KxvDirectory.emitString(): String {
    val fileImportsB = classes.joinToString("\n") { "import ${it.packageName}.${it.reflectiveObjectName}" }
    return """
            package ${qualifiedValName.substringBeforeLast('.')}

            import com.lightningkite.kotlinx.reflection.kxReflect

            $fileImportsB

            val ${qualifiedValName.substringAfterLast('.')} = listOf(${classes.joinToString(",\n") { it.reflectiveObjectName }}
            )
        """.trimIndent()
}

fun KxvFile.emitString(): String {
    return """
            package $packageName

            import com.lightningkite.kotlinx.reflection.*
            ${generalImports.joinToString("\n") { "import $it.*" }}
            ${specificImports.joinToString("\n") { "import $it" }}

            ${classes.joinToString("\n\n") { it.emitString() }}
        """.trimIndent()
}

fun KxvClass.emitString(): String {
    anyfy()
    val selfType = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection.STAR })
    val selfTypeAny = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection(KxvType("Any", true)) })
    val typeParametersRegexes by lazy {
        typeParameters.map { Regex("\\b$it\\b") }
    }
    val typeParametersClassRegexes by lazy {
        typeParameters.map { Regex("\\b$it::class\\b") }
    }

    fun String.preventTypeParams(): String {
        return this.let {
            typeParametersClassRegexes.fold(it) { acc, typeParam ->
                acc.replace(typeParam, "Any::class")
            }
        }.let {
            typeParametersRegexes.fold(it) { acc, typeParam ->
                acc.replace(typeParam, "Any?")
            }
        }.replace("Any??", "Any?")
    }
    val implementsDeclarations: String = implements.joinToString(",", "listOf<KxType>(", ")") { it.emitString() }

    val valDeclarations: String = variables.entries.filter { !it.value.mutable }.joinToString("\n") {
        """val ${it.key} by lazy { ${it.value.emitString(this)} }"""
    }.preventTypeParams()
    val valMap: String = variables.entries.filter { !it.value.mutable }.joinToString(", ", "mapOf<String, KxValue<${selfType.emitStringActual()}, *>>(", ")") {
        """"${it.key}" to ${it.key}"""
    }.preventTypeParams()

    val varDeclarations: String = variables.entries.filter { it.value.mutable }.joinToString("\n") {
        """val ${it.key} by lazy { ${it.value.emitString(this)} }"""
    }.preventTypeParams()
    val varMap: String = variables.entries.filter { it.value.mutable }.joinToString(", ", "mapOf<String, KxVariable<${selfType.emitStringActual()}, *>>(", ")") {
        """"${it.key}" to ${it.key}"""
    }.preventTypeParams()

    val functionList: String = functions.joinToString(", ", "listOf<KxFunction<*>>(", ")") {
        it.emitString()
    }.preventTypeParams()
    val constructorList: String = constructors.joinToString(", ", "listOf<KxFunction<${selfType.emitStringActual()}>>(", ")") {
        it.emitString()
    }.preventTypeParams()
    val annotationList: String = annotations.joinToString(", ", "listOf(", ")") {
        it.emitString()
    }
    val enumValuesText = enumValues?.joinToString(",", "listOf(", ")") { "$simpleName.$it" }
    return """
            object $reflectiveObjectName: KxClass<${selfType.emitStringActual()}>{

                $valDeclarations

                $varDeclarations

                override val kclass get() = $simpleName::class

                override val implements: List<KxType> by lazy{ $implementsDeclarations }

                override val simpleName: String = "$simpleName"
                override val qualifiedName: String = "$qualifiedName"
                override val values: Map<String, KxValue<${selfType.emitStringActual()}, *>> by lazy { $valMap }
                override val variables: Map<String, KxVariable<${selfType.emitStringActual()}, *>> by lazy { $varMap }
                override val functions: List<KxFunction<*>> by lazy { $functionList }
                override val constructors: List<KxFunction<${selfType.emitStringActual()}>> by lazy { $constructorList }
                override val annotations: List<KxAnnotation> = $annotationList

                override val modifiers: List<KxClassModifier> = listOf(${modifiers.joinToString { "KxClassModifier." + it.name }})
                override val enumValues: List<${selfType.emitStringActual()}>? = $enumValuesText
            }
        """.trimIndent()
}


fun KxvTypeProjection.emitStringActual(): String = if (isStar) "*"
else when (variance) {
    KxVariance.INVARIANT -> type.emitStringActual()
    KxVariance.IN -> "in " + type.emitStringActual()
    KxVariance.OUT -> "out " + type.emitStringActual()
}

fun KxvTypeProjection.emitStringActualNotNull(): String = if (isStar) "*"
else when (variance) {
    KxVariance.INVARIANT -> type.emitStringActualNotNull()
    KxVariance.IN -> "in " + type.emitStringActualNotNull()
    KxVariance.OUT -> "out " + type.emitStringActualNotNull()
}

fun KxvTypeProjection.emitString(): String = if (isStar) "KxTypeProjection.STAR"
else """
        KxTypeProjection(
            type = ${type.emitString()},
            variance = KxVariance.${variance.name}
        )
    """.trimIndent()

fun KxvType.emitStringActual(): String {
    return emitStringActualNotNull() + if (nullable) "?" else ""
}

fun KxvType.emitStringActualNotNull(): String {
    return if (typeParameters.isEmpty()) {
        base
    } else {
        base + typeParameters.joinToString(", ", "<", ">") { it.emitStringActual() }
    }
}

fun KxvType.emitString(): String {
    val typeParametersText = typeParameters.joinToString(", ", "listOf(", ")") { it.emitString() }
    val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.emitString() }
    return """
            KxType(
                base = $base::class.kxReflect,
                nullable = $nullable,
                typeParameters = $typeParametersText,
                annotations = $annotationText
            )
        """.trimIndent()
}

fun KxvVariable.emitString(owner: KxvClass): String {
    val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.emitString() }

    if (mutable) {
        val setText = if (owner.typeParameters.isNotEmpty()) {
            """
                    { owner, value ->
                        @Suppress("UNCHECKED_CAST")
                        (owner as ${owner.selfTypeAny.emitStringActual()}).$name = (value as ${type.emitStringActual()})
                    }
                """.trimIndent()
        } else {
            """{ owner, value -> owner.$name = value }"""
        }
        return """
            KxVariable<${owner.selfType.emitStringActual()}, ${type.emitStringActual()}>(
                owner = this,
                name = "$name",
                type = ${type.emitString()},
                get = { owner -> owner.$name as ${type.emitStringActual()} },
                set = $setText,
                annotations = $annotationText
            )
        """.trimIndent()
    } else {
        return """
            KxValue<${owner.selfType.emitStringActual()}, ${type.emitStringActual()}>(
                owner = this,
                name = "$name",
                type = ${type.emitString()},
                get = { owner -> owner.$name as ${type.emitStringActual()} },
                annotations = $annotationText
            )
        """.trimIndent()
    }
}

fun KxvArgument.emitString(arguments: List<KxvArgument>): String {
    val defaultText = default?.let {
        var current = it
        arguments.forEachIndexed { index, argument ->
            current = current.replace(Regex("\\b" + argument.name + "\\b"), "(previousArguments[$index] as ${argument.type.emitStringActual()})")
        }
        "{ previousArguments -> $current }"
    } ?: "null"
    val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.emitString() }
    return """
            KxArgument(
                name = "$name",
                type = ${type.emitString()},
                annotations = $annotationText,
                default = $defaultText
            )
        """.trimIndent()
}
fun KxvFunction.emitString(): String {
    val argumentsText = arguments.joinToString(", ", "listOf(", ")") { it.emitString(arguments) }
    val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.emitString() }
    return """
            KxFunction<${type.emitStringActual()}>(
                name = "$name",
                type = ${type.emitString()},
                arguments = $argumentsText,
                call = $callCode,
                annotations = $annotationText
            )
        """.trimIndent()
}
val KxvFunction.callCode: String get(){
    val argString = arguments.indices.joinToString { "it[$it] as ${arguments[it].type.emitStringActual()}" }
    return if (typeParameters.isEmpty()) {
        "{ $name($argString) }"
    } else {
        "{ $name<${typeParameters.joinToString { it.minimum.emitStringActual() }}>($argString) }"
    }
}
fun KxvAnnotation.emitString(): String {
    return """
            KxAnnotation(
                name = "$name",
                arguments = listOf(${arguments.joinToString()})
            )
        """.trimIndent()
}