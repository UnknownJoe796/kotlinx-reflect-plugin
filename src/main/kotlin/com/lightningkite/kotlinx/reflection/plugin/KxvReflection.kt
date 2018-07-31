package com.lightningkite.kotlinx.reflection.plugin

import com.lightningkite.kotlinx.reflection.KxVariance

data class KxvFile(
        val fileName: String,
        val packageName: String,
        val generalImports: List<String>,
        val specificImports: List<String>,
        val classes: List<KxvClass>
) {
    fun write(): String {
        return """
            package $packageName

            import com.lightningkite.kotlinx.reflection.*
            ${generalImports.joinToString("\n") { "import $it.*" }}
            ${specificImports.joinToString("\n") { "import $it" }}

            ${classes.joinToString("\n\n") { it.write() }}
        """.trimIndent()
    }
}

data class KxvClass(
        val simpleName: String,
        val qualifiedName: String,
        val typeParameters: List<String>,
        val variables: Map<String, KxvVariable> = mapOf(),
        val functions: List<KxvFunction> = listOf(),
        val constructors: List<KxvFunction> = listOf(),
        val annotations: List<KxvAnnotation> = listOf(),
        val isInterface: Boolean = false,
        val isOpen: Boolean = false,
        val isAbstract: Boolean = false,
        val enumValues: List<String>? = null
) {
    val selfType = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection.STAR })
    val selfTypeAny = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection(KxvType("Any", true)) })
    private val typeParametersRegexes by lazy {
        typeParameters.map { Regex("\\b$it\\b") }
    }
    private val typeParametersClassRegexes by lazy {
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

    fun write(): String {
        val valDeclarations: String = variables.entries.filter { !it.value.mutable }.joinToString("\n") {
            """val ${it.key} = ${it.value.write(this)}"""
        }.preventTypeParams()
        val valMap: String = variables.entries.filter { !it.value.mutable }.joinToString(", ", "mapOf(", ")") {
            """"${it.key}" to ${it.key}"""
        }.preventTypeParams()

        val varDeclarations: String = variables.entries.filter { it.value.mutable }.joinToString("\n") {
            """val ${it.key} = ${it.value.write(this)}"""
        }.preventTypeParams()
        val varMap: String = variables.entries.filter { it.value.mutable }.joinToString(", ", "mapOf(", ")") {
            """"${it.key}" to ${it.key}"""
        }.preventTypeParams()

        val functionList: String = functions.joinToString(", ", "listOf(", ")") {
            it.write()
        }.preventTypeParams()
        val constructorList: String = constructors.joinToString(", ", "listOf(", ")") {
            it.write()
        }.preventTypeParams()
        val annotationList: String = annotations.joinToString(", ", "listOf(", ")") {
            it.write()
        }
        val enumValuesText = enumValues?.joinToString(",", "listOf(", ")") { "$simpleName.$it" }
        return """
            object ${simpleName}Reflection: KxClass<${selfType.writeActual()}>{

                $valDeclarations

                $varDeclarations

                override val kclass get() = $simpleName::class

                override val simpleName: String = "$simpleName"
                override val qualifiedName: String = "$qualifiedName"
                override val values: Map<String, KxValue<${selfType.writeActual()}, *>> = $valMap
                override val variables: Map<String, KxVariable<${selfType.writeActual()}, *>> = $varMap
                override val functions: List<KxFunction<*>> = $functionList
                override val constructors: List<KxFunction<${selfType.writeActual()}>> = $constructorList
                override val annotations: List<KxAnnotation> = $annotationList

                override val isInterface: Boolean get() = $isInterface
                override val isOpen: Boolean get() = $isOpen
                override val isAbstract: Boolean get() = $isAbstract
                override val enumValues: List<$simpleName>? = $enumValuesText
            }
        """.trimIndent()
    }
}

data class KxvTypeProjection(
        val type: KxvType,
        val variance: KxVariance = KxVariance.INVARIANT,
        val isStar: Boolean = false
) {
    companion object {
        val STAR = KxvTypeProjection(KxvType("Any", true), isStar = true)
    }

    fun writeActual(): String = if (isStar) "*"
    else when (variance) {
        KxVariance.INVARIANT -> type.writeActual()
        KxVariance.IN -> "in " + type.writeActual()
        KxVariance.OUT -> "out " + type.writeActual()
    }

    fun writeActualNotNull(): String = if (isStar) "*"
    else when (variance) {
        KxVariance.INVARIANT -> type.writeActualNotNull()
        KxVariance.IN -> "in " + type.writeActualNotNull()
        KxVariance.OUT -> "out " + type.writeActualNotNull()
    }

    fun write(): String = if (isStar) "KxTypeProjection.STAR"
    else """
        KxTypeProjection(
            type = ${type.write()},
            variance = KxVariance.${variance.name}
        )
    """.trimIndent()
}


data class KxvType(
        val base: String,
        val nullable: Boolean,
        val typeParameters: List<KxvTypeProjection> = listOf(),
        val annotations: List<KxvAnnotation> = listOf()
) {
    fun writeActual(): String {
        return writeActualNotNull() + if (nullable) "?" else ""
    }

    fun writeActualNotNull(): String {
        return if (typeParameters.isEmpty()) {
            base
        } else {
            base + typeParameters.joinToString(", ", "<", ">") { it.writeActual() }
        }
    }

    fun write(): String {
        val typeParametersText = typeParameters.joinToString(", ", "listOf(", ")") { it.write() }
        val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.write() }
        return """
            KxType(
                base = $base::class.kxReflect,
                nullable = $nullable,
                typeParameters = $typeParametersText,
                annotations = $annotationText
            )
        """.trimIndent()
    }
}

data class KxvVariable(
        val name: String,
        val type: KxvType,
        val mutable: Boolean,
        val artificial: Boolean,
        val annotations: List<KxvAnnotation>
) {
    fun write(owner: KxvClass): String {
        val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.write() }

        if (mutable) {
            val setText = if (owner.typeParameters.isNotEmpty()) {
                """
                    { owner, value ->
                        @Suppress("UNCHECKED_CAST")
                        (owner as ${owner.selfTypeAny.writeActual()}).$name = (value as ${type.writeActual()})
                    }
                """.trimIndent()
            } else {
                """{ owner, value -> owner.$name = value }"""
            }
            return """
            KxVariable<${owner.selfType.writeActual()}, ${type.writeActual()}>(
                name = "$name",
                type = ${type.write()},
                get = { owner -> owner.$name as ${type.writeActual()} },
                set = $setText,
                annotations = $annotationText
            )
        """.trimIndent()
        } else {
            return """
            KxValue<${owner.selfType.writeActual()}, ${type.writeActual()}>(
                name = "$name",
                type = ${type.write()},
                get = { owner -> owner.$name as ${type.writeActual()} },
                annotations = $annotationText
            )
        """.trimIndent()
        }
    }
}

data class KxvArgument(
        val name: String,
        val type: KxvType,
        val annotations: List<KxvAnnotation>,
        val default: String?
) {
    fun write(arguments: List<KxvArgument>): String {
        val defaultText = default?.let {
            var current = it
            arguments.forEachIndexed { index, argument ->
                current = current.replace(Regex("\\b" + argument.name + "\\b"), "(previousArguments[$index] as ${argument.type.writeActual()})")
            }
            "{ previousArguments -> $current }"
        } ?: "null"
        val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.write() }
        return """
            KxArgument(
                name = "$name",
                type = ${type.write()},
                annotations = $annotationText,
                default = $defaultText
            )
        """.trimIndent()
    }
}

data class KxvFunction(
        val name: String,
        val type: KxvType,
        val arguments: List<KxvArgument>,
        val annotations: List<KxvAnnotation>,
        val callCode: String
) {
    fun write(): String {
        val argumentsText = arguments.joinToString(", ", "listOf(", ")") { it.write(arguments) }
        val annotationText = annotations.joinToString(", ", "listOf(", ")") { it.write() }
        return """
            KxFunction<${type.writeActual()}>(
                name = "$name",
                type = ${type.write()},
                arguments = $argumentsText,
                call = $callCode,
                annotations = $annotationText
            )
        """.trimIndent()
    }
}

data class KxvAnnotation(
        val name: String,
        val arguments: List<String>
) {
    fun write(): String {
        return """
            KxAnnotation(
                name = "$name",
                arguments = listOf(${arguments.joinToString()})
            )
        """.trimIndent()
    }
}