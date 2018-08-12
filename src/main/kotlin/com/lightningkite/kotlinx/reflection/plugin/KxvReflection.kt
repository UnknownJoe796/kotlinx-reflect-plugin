package com.lightningkite.kotlinx.reflection.plugin

data class KxvFile(
        val fileName: String,
        val packageName: String,
        val generalImports: List<String>,
        val specificImports: List<String>,
        val classes: List<KxvClass>
)

data class KxvClass(
        val simpleName: String,
        val qualifiedName: String,
        val implements: List<KxvType>,
        val typeParameters: List<String>,
        val variables: Map<String, KxvVariable> = mapOf(),
        val functions: List<KxvFunction> = listOf(),
        val constructors: List<KxvFunction> = listOf(),
        val annotations: List<KxvAnnotation> = listOf(),
        val modifiers: List<KxClassModifier> = listOf(),
        val enumValues: List<String>? = null
){
    val selfType = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection.STAR })
    val selfTypeAny = KxvType(simpleName, false, (0 until typeParameters.size).map { KxvTypeProjection(KxvType("Any", true)) })
}

data class KxvTypeProjection(
        val type: KxvType,
        val variance: KxVariance = KxVariance.INVARIANT,
        val isStar: Boolean = false
) {
    companion object {
        val STAR = KxvTypeProjection(KxvType("Any", true), isStar = true)
    }
}


data class KxvType(
        val base: String,
        val nullable: Boolean,
        val typeParameters: List<KxvTypeProjection> = listOf(),
        val annotations: List<KxvAnnotation> = listOf()
)

data class KxvVariable(
        val name: String,
        val type: KxvType,
        val mutable: Boolean,
        val artificial: Boolean,
        val annotations: List<KxvAnnotation>
)

data class KxvArgument(
        val name: String,
        val type: KxvType,
        val annotations: List<KxvAnnotation>,
        val default: String?
)

data class KxvFunction(
        val name: String,
        val type: KxvType,
        val arguments: List<KxvArgument>,
        val annotations: List<KxvAnnotation>,
        val callCode: String
)

data class KxvAnnotation(
        val name: String,
        val arguments: List<String>,
        val useSiteTarget: String? = null
)