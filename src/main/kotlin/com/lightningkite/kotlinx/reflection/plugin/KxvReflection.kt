package com.lightningkite.kotlinx.reflection.plugin

interface KxvNavigable{
    fun subs():Sequence<KxvNavigable>
}

fun KxvNavigable.recursiveSubs() = subs().recursiveFlatMap { it.subs() }

class KxvDirectory(val qualifiedValName: String, val classes: List<KxvClass>)

data class KxvFile(
        var fileName: String,
        var packageName: String,
        var generalImports: List<String>,
        var specificImports: List<String>,
        var classes: List<KxvClass>
): KxvNavigable{
    override fun subs(): Sequence<KxvNavigable>
        = classes.asSequence()
}

data class KxvClass(
        var simpleName: String,
        var packageName: String,
        var implements: List<KxvType>,
        var typeParameters: List<KxvTypeParameter>,
        var variables: Map<String, KxvVariable> = mapOf(),
        var functions: List<KxvFunction> = listOf(),
        var constructors: List<KxvFunction> = listOf(),
        var annotations: List<KxvAnnotation> = listOf(),
        var modifiers: List<KxClassModifier> = listOf(),
        var enumValues: List<String>? = null
): KxvNavigable{
    val qualifiedName get() = "$packageName.$simpleName"
    val reflectiveObjectName get() = simpleName.filter { it.isJavaIdentifierPart() } + "Reflection"
    var selfType = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection.STAR })
    var selfTypeAny = KxvType(simpleName, false, typeParameters.map { KxvTypeProjection(it.minimum) })
    override fun subs(): Sequence<KxvNavigable>
            = implements.asSequence() +
            variables.values.asSequence() +
//            typeParameters.asSequence() +
            functions.asSequence() +
            constructors.asSequence() +
            annotations.asSequence()
}

data class KxvTypeParameter(
        var name: String,
        var minimum: KxvType = KxvType("Any", true),
        var variance: KxVariance = KxVariance.INVARIANT
): KxvNavigable {
    override fun subs(): Sequence<KxvNavigable>
            = sequenceOf(minimum)
}

data class KxvTypeProjection(
        var type: KxvType,
        var variance: KxVariance = KxVariance.INVARIANT,
        var isStar: Boolean = false
): KxvNavigable {
    companion object {
        var STAR = KxvTypeProjection(KxvType("Any", true), isStar = true)
    }

    fun deepCopy(): KxvTypeProjection = copy(type = type.deepCopy())

    override fun subs(): Sequence<KxvNavigable>
            = sequenceOf(type)
}


data class KxvType(
        var base: String,
        var nullable: Boolean = false,
        var typeParameters: List<KxvTypeProjection> = listOf(),
        var annotations: List<KxvAnnotation> = listOf()
): KxvNavigable {
    override fun subs(): Sequence<KxvNavigable>
            = typeParameters.asSequence() + annotations.asSequence()
    fun deepCopy() = copy(typeParameters = typeParameters.map { it.deepCopy() })
}

data class KxvVariable(
        var name: String,
        var type: KxvType,
        var mutable: Boolean,
        var artificial: Boolean,
        var annotations: List<KxvAnnotation>
): KxvNavigable {
    override fun subs(): Sequence<KxvNavigable>
            = annotations.asSequence() + sequenceOf(type)
}

data class KxvArgument(
        var name: String,
        var type: KxvType,
        var annotations: List<KxvAnnotation>,
        var default: String?
): KxvNavigable {
    override fun subs(): Sequence<KxvNavigable>
            = sequenceOf(type) + annotations.asSequence()
}

data class KxvFunction(
        var name: String,
        var type: KxvType,
        var typeParameters: List<KxvTypeParameter>,
        var arguments: List<KxvArgument>,
        var annotations: List<KxvAnnotation>
): KxvNavigable{
    override fun subs(): Sequence<KxvNavigable>
            = sequenceOf(type) + annotations.asSequence() + arguments.asSequence()
}

data class KxvAnnotation(
        var name: String,
        var arguments: List<String>,
        var useSiteTarget: String? = null
): KxvNavigable{
    override fun subs(): Sequence<KxvNavigable> = sequenceOf()
}