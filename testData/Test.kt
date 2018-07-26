package com.lightningkite.kotlinx.reflection.plugin.test

import com.lightningkite.kotlinx.reflection.ExternalReflection
import java.util.*

@ExternalReflection
class TestClass<T>(
        var name: String,
        var description: String = "",
        var nestMaybe: TestClass<*>? = null,
        normalParam: Int = 2
) {

    var anotherVar: T? = null

    var child: TestClass<T>? = null

    var childInt: TestClass<Int?>? = null

    val created: Date = Date()

    val somethingElse get() = 3

    fun function(param: Int = 0): Int {
        TestClassReflection.name.set(this, "mah new name")
        return param
    }
}

interface TestInterface {
    fun test()
}