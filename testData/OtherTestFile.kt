package com.lightningkite.kotlinx.reflection.plugin.test

@ExternalReflection
data class TestClass(
        var a: Int = 42,
        var b: String = "string"
)