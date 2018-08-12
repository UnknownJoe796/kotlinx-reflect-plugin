package com.lightningkite.kotlinx.reflection.plugin

enum class KxClassModifier {
    Sealed,
    Abstract,
    Data,
    Open,
    Interface
}

val KxClassModifierMap = KxClassModifier.values().associate { it.name.toLowerCase() to it }