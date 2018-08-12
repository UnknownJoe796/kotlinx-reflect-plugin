package com.lightningkite.kotlinx.reflection.plugin

fun Node.getFile(fileName: String): KxvFile {
    val packageName = this["packageHeader"]?.get("identifier")?.toStringIdentifier() ?: ""
    val classes = this.children
            .filter { it.type == "topLevelObject" }
            .mapNotNull { it["classDeclaration"]?.toKxClasses(packageName) }
            .flatMap { it }
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

fun Node.toStringIdentifier() = children.filter { it.type == "simpleIdentifier" }
        .joinToString(".") { it.content!! }

fun Node.toKxClasses(packageName: String, owner: KxvClass? = null): List<KxvClass> {
    val directName = this["simpleIdentifier"]!!.content!!
    val simpleName = owner?.let{
        it.simpleName + "." + directName
    } ?: directName
    val constructorVarList = this["primaryConstructor"]
            ?.get("classParameters")
            ?.children?.filter { it.type == "classParameter" }
            ?.mapNotNull { it.toKxConstructorVariable() } ?: listOf()
    val normalVarList = (this["classBody"] ?: this["enumClassBody"])?.children
            ?.filter { it.type == "classMemberDeclaration" }
            ?.mapNotNull { it["propertyDeclaration"] }
            ?.map { it.toKxVariable() } ?: listOf()
    val typeParams = get("typeParameters")?.children
            ?.filter { it.type == "typeParameter" }
            ?.map {
                KxvTypeParameter(
                        name = it["simpleIdentifier"]!!.content!!,
                        minimum = it["type"]?.toKxType() ?: KxvType("Any", true)
                )
            }
            ?: listOf()
    val implementsList = get("delegationSpecifiers")?.children?.mapNotNull {
        it.get("delegationSpecifier")?.let{
            it.get("constructorInvocation")?.get("userType")?.toKxType() ?:
                    it.get("userType")?.toKxType()
        }
    } ?: listOf()
    val currentClass = KxvClass(
            simpleName = simpleName,
            packageName = packageName,
            implements = implementsList,
            typeParameters = typeParams,
            variables = (constructorVarList + normalVarList).associate { it.name to it },
            functions = listOf(),
            constructors = listOfNotNull(this["primaryConstructor"]?.toKxConstructor(simpleName, typeParams)),
            annotations = kxAnnotationsFromModifierList("class"),
            modifiers = (this["modifierList"]?.children?.filter { it.type == "modifier" }?.mapNotNull { KxClassModifierMap[it.content] }
                    ?: listOf()) + (
                    if (this.terminals.contains("interface"))
                        listOf(KxClassModifier.Interface)
                    else
                        listOf()
                    ),
            enumValues = this["enumClassBody"]?.get("enumEntries")?.children?.mapNotNull { it["simpleIdentifier"]?.content }
    )
    val subclasses:List<KxvClass> = this["classBody"]?.children
            ?.filter { it.type == "classMemberDeclaration" }
            ?.mapNotNull { it["classDeclaration"]?.toKxClasses(packageName, currentClass) }
            ?.flatMap { it }
            ?: listOf()

    return subclasses + currentClass
}

fun Node.kxAnnotationsFromModifierList(targeting: String): List<KxvAnnotation> {
    return get("modifierList")?.get("annotations")?.children?.map { it.toKxAnnotation() }?.filter { it.useSiteTarget == null || it.useSiteTarget == targeting }
            ?: listOf()
}

fun Node.toKxAnnotation(): KxvAnnotation {
    return KxvAnnotation(
            name = this["unescapedAnnotation"]?.get("identifier")?.toStringIdentifier() ?: terminals[0].drop(1),
            arguments = this["valueArguments"]?.children?.mapNotNull {
                it["valueArgument"]?.get("expression")?.content
            } ?: listOf(),
            useSiteTarget = this["annotationUseSiteTarget"]?.terminals?.firstOrNull()
    )
}

val anyNullableType = KxvType("Any", true, listOf(), listOf())
fun Node.toKxType(annotations: List<KxvAnnotation> = listOf()): KxvType {
    return when (type) {
        "nullableType" -> this.children.firstOrNull()?.toKxType()?.copy(nullable = true)
                ?: anyNullableType
        "type", "typeProjection", "typeReference" -> this.children.firstOrNull()?.toKxType()
                ?: anyNullableType
        "userType" -> {
            if (children.count { it.type == "simpleUserType" } == 1) get("simpleUserType")!!.toKxType()
            else KxvType(
                    base = toStringIdentifier(),
                    typeParameters = children.lastOrNull()
                            ?.get("typeArguments")
                            ?.children
                            ?.map { it.toKxTypeProjection() }
                            ?: listOf(),
                    nullable = false
            )
        }
        "simpleUserType" -> KxvType(
                base = toStringIdentifier(),
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

fun Node.toKxConstructor(forName: String, typeParams: List<KxvTypeParameter>): KxvFunction {
    val args = this["classParameters"]!!.children.map { it.toKxConstructorParam() }
    return KxvFunction(
            name = forName,
            type = KxvType(forName, false, typeParams.map { KxvTypeProjection.STAR }, listOf()),
            typeParameters = typeParams.map { it.copy() },
            arguments = args,
            annotations = listOf()
    )
}

fun Node.toKxConstructorParam(): KxvArgument = KxvArgument(
        name = this["simpleIdentifier"]!!.content!!,
        type = this["type"]!!.toKxType(),
        annotations = listOf(),
        default = this["expression"]?.content
)

fun Node.toKxConstructorVariable(): KxvVariable? {
    if (!terminals.contains("var") && !terminals.contains("val")) return null
    return KxvVariable(
            name = this["simpleIdentifier"]!!.content!!,
            type = this["type"]!!.toKxType(),
            annotations = kxAnnotationsFromModifierList("property"),
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
            annotations = kxAnnotationsFromModifierList("property")
    )
}