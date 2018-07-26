package com.lightningkite.kotlinx.reflection.plugin

import com.lightningkite.kotlinx.reflection.KxVariance

fun Node.getFile(fileName: String): KxvFile {
    val packageName = this["packageHeader"]?.get("identifier")
            ?.children?.filter { it.type == "simpleIdentifier" }
            ?.joinToString(".") { it.content!! } ?: ""
    val classes = this.children
            .filter { it.type == "topLevelObject" }
            .mapNotNull { it["classDeclaration"]?.toKxClass(packageName) }
    val generalImports = this["importList"]?.children?.mapNotNull {
        if (it.terminals.contains("*")) {
            it["identifier"]!!.children.joinToString(".") { it.content!! }
        } else null
    } ?: listOf()
    val specificImports = this["importList"]?.children?.mapNotNull {
        if (!it.terminals.contains("*")) {
            it["identifier"]!!.children.joinToString(".") { it.content!! }
        } else null
    } ?: listOf()
    return KxvFile(
            fileName = fileName,
            packageName = packageName,
            specificImports = specificImports,
            generalImports = generalImports,
            classes = classes
    )
}

fun Node.toKxClass(packageName: String): KxvClass {
    val simpleName = this["simpleIdentifier"]!!.content!!
    val constructorVarList = this["primaryConstructor"]
            ?.get("classParameters")
            ?.children?.filter { it.type == "classParameter" }
            ?.mapNotNull { it.toKxConstructorVariable() } ?: listOf()
    val normalVarList = this.get("classBody")?.children
            ?.filter { it.type == "classMemberDeclaration" }
            ?.mapNotNull { it["propertyDeclaration"] }
            ?.map { it.toKxVariable() } ?: listOf()
    val typeParams = get("typeParameters")?.children
            ?.filter { it.type == "typeParameter" }
            ?.mapNotNull { it["simpleIdentifier"]?.content }
            ?: listOf()
    return KxvClass(
            simpleName = simpleName,
            qualifiedName = "$packageName.$simpleName",
            typeParameters = typeParams,
            variables = (constructorVarList + normalVarList).associate { it.name to it },
            functions = listOf(),
            constructors = listOfNotNull(this["primaryConstructor"]?.toKxConstructor(simpleName, typeParams)),
            annotations = get("modifierList")?.get("annotations")?.children?.map { it.toKxAnnotation() }
                    ?: listOf()
    )
}

fun Node.toKxAnnotation(): KxvAnnotation {
    return KxvAnnotation(
            name = terminals[0].drop(1),
            arguments = this["valueArguments"]?.children?.mapNotNull {
                it["valueArgument"]?.get("expression")?.content
            } ?: listOf()
    )
}

val anyNullableType = KxvType("Any", true, listOf(), listOf())
fun Node.toKxType(annotations: List<KxvAnnotation> = listOf()): KxvType {
    return when (type) {
        "nullableType" -> this.children.firstOrNull()?.toKxType()?.copy(nullable = true)
                ?: anyNullableType
        "type", "typeProjection", "userType", "typeReference" -> this.children.firstOrNull()?.toKxType()
                ?: anyNullableType
        "simpleUserType" -> KxvType(
                base = this["simpleIdentifier"]!!.content!!,
                nullable = false,
                typeParameters = this["typeArguments"]?.children?.map { it.toKxTypeProjection() }
                        ?: listOf(),
                annotations = annotations
        )
        else -> anyNullableType
    }
}

fun Node.toKxTypeProjection(annotations: List<KxvAnnotation> = listOf()): KxvTypeProjection {
    return KxvTypeProjection(
            type = this["type"]?.toKxType(annotations)
                    ?: anyNullableType,
            variance = this["typeProjectionModifierList"]?.get("varianceAnnotation")?.terminals?.firstOrNull()?.let { KxVariance.valueOf(it.toUpperCase()) }
                    ?: KxVariance.INVARIANT,
            isStar = this["type"] == null
    )
}

fun Node.toKxConstructor(forName: String, typeParams: List<String>): KxvFunction {
    val args = this["classParameters"]!!.children.map { it.toKxConstructorParam() }
    val argString = args.indices.joinToString { "it[$it] as ${args[it].type.writeActual()}" }
    val callCode = if (typeParams.isEmpty()) {
        "{ $forName($argString) }"
    } else {
        "{ $forName<${typeParams.indices.joinToString { "Any?" }}>($argString) }"
    }
    return KxvFunction(
            name = "",
            type = KxvType(forName, false, typeParams.indices.map { KxvTypeProjection.STAR }, listOf()),
            arguments = args,
            annotations = listOf(),
            callCode = callCode
    )
}

fun Node.toKxConstructorParam(): KxvArgument = KxvArgument(
        name = this["simpleIdentifier"]!!.content!!,
        type = this["type"]!!.toKxType(),
        annotations = listOf()
)

fun Node.toKxConstructorVariable(): KxvVariable? {
    if (!terminals.contains("var") || terminals.contains("val")) return null
    return KxvVariable(
            name = this["simpleIdentifier"]!!.content!!,
            type = this["type"]!!.toKxType(),
            annotations = listOf(),
            artificial = false,
            mutable = terminals.contains("var")
    )
}

fun Node.toKxVariable(): KxvVariable {
    val varDec = this["variableDeclaration"]!!
    return KxvVariable(
            name = varDec["simpleIdentifier"]!!.content!!,
            type = varDec["type"]?.toKxType()
                    ?: KxvType("Any", true, listOf(), listOf()),
            mutable = this.terminals.contains("var"),
            artificial = this["getter"] != null,
            annotations = listOf()
    )
}