package com.lightningkite.kotlinx.reflection.plugin.test

@ExternalReflection
data class TestClass(
        @get:java.beans.Transient var a: Int = 42,
        @get:Deprecated("REASONS") var b: String = "string"
)