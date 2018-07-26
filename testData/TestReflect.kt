package com.lightningkite.kotlinx.reflection.plugin.test

import com.lightningkite.kotlinx.reflection.*
import java.util.*
import com.lightningkite.kotlinx.reflection.KxValue

object TestClassReflection : KxClass<TestClass<*>> {

    val created = KxValue<TestClass<*>, Date>(
            name = "created",
            type = KxType(
                    base = Date::class.kxReflect,
                    nullable = false,
                    typeParameters = listOf(),
                    annotations = listOf()
            ),
            get = { owner -> owner.created as Date },
            annotations = listOf()
    )
    val somethingElse = KxValue<TestClass<*>, Any?>(
            name = "somethingElse",
            type = KxType(
                    base = Any::class.kxReflect,
                    nullable = true,
                    typeParameters = listOf(),
                    annotations = listOf()
            ),
            get = { owner -> owner.somethingElse as Any? },
            annotations = listOf()
    )

    val name = KxVariable<TestClass<*>, String>(
            name = "name",
            type = KxType(
                    base = String::class.kxReflect,
                    nullable = false,
                    typeParameters = listOf(),
                    annotations = listOf()
            ),
            get = { owner -> owner.name as String },
            set = { owner, value ->
                @Suppress("UNCHECKED_CAST")
                (owner as TestClass<Any?>).name = (value as String)
            },
            annotations = listOf()
    )
    val description = KxVariable<TestClass<*>, String>(
            name = "description",
            type = KxType(
                    base = String::class.kxReflect,
                    nullable = false,
                    typeParameters = listOf(),
                    annotations = listOf()
            ),
            get = { owner -> owner.description as String },
            set = { owner, value ->
                @Suppress("UNCHECKED_CAST")
                (owner as TestClass<Any?>).description = (value as String)
            },
            annotations = listOf()
    )
    val nestMaybe = KxVariable<TestClass<*>, TestClass<*>?>(
            name = "nestMaybe",
            type = KxType(
                    base = TestClass::class.kxReflect,
                    nullable = true,
                    typeParameters = listOf(KxTypeProjection.STAR),
                    annotations = listOf()
            ),
            get = { owner -> owner.nestMaybe as TestClass<*>? },
            set = { owner, value ->
                @Suppress("UNCHECKED_CAST")
                (owner as TestClass<Any?>).nestMaybe = (value as TestClass<*>?)
            },
            annotations = listOf()
    )
    val anotherVar = KxVariable<TestClass<*>, Any?>(
            name = "anotherVar",
            type = KxType(
                    base = Any::class.kxReflect,
                    nullable = true,
                    typeParameters = listOf(),
                    annotations = listOf()
            ),
            get = { owner -> owner.anotherVar as Any? },
            set = { owner, value ->
                @Suppress("UNCHECKED_CAST")
                (owner as TestClass<Any?>).anotherVar = (value as Any?)
            },
            annotations = listOf()
    )
    val child = KxVariable<TestClass<*>, TestClass<Any?>?>(
            name = "child",
            type = KxType(
                    base = TestClass::class.kxReflect,
                    nullable = true,
                    typeParameters = listOf(KxTypeProjection(
                            type = KxType(
                                    base = Any::class.kxReflect,
                                    nullable = false,
                                    typeParameters = listOf(),
                                    annotations = listOf()
                            ),
                            variance = KxVariance.INVARIANT
                    )),
                    annotations = listOf()
            ),
            get = { owner -> owner.child as TestClass<Any?>? },
            set = { owner, value ->
                @Suppress("UNCHECKED_CAST")
                (owner as TestClass<Any?>).child = (value as TestClass<Any?>?)
            },
            annotations = listOf()
    )
    val childInt = KxVariable<TestClass<*>, TestClass<Int?>?>(
            name = "childInt",
            type = KxType(
                    base = TestClass::class.kxReflect,
                    nullable = true,
                    typeParameters = listOf(KxTypeProjection(
                            type = KxType(
                                    base = Int::class.kxReflect,
                                    nullable = true,
                                    typeParameters = listOf(),
                                    annotations = listOf()
                            ),
                            variance = KxVariance.INVARIANT
                    )),
                    annotations = listOf()
            ),
            get = { owner -> owner.childInt as TestClass<Int?>? },
            set = { owner, value ->
                @Suppress("UNCHECKED_CAST")
                (owner as TestClass<Any?>).childInt = (value as TestClass<Int?>?)
            },
            annotations = listOf()
    )

    override val simpleName: String = "TestClass"
    override val qualifiedName: String = "com.lightningkite.kotlinx.reflect.properties.gradle.test.TestClass"
    override val values: Map<String, KxValue<TestClass<*>, *>> = mapOf("created" to created, "somethingElse" to somethingElse)
    override val variables: Map<String, KxVariable<TestClass<*>, *>> = mapOf("name" to name, "description" to description, "nestMaybe" to nestMaybe, "anotherVar" to anotherVar, "child" to child, "childInt" to childInt)
    override val functions: List<KxFunction<*>> = listOf()
    override val constructors: List<KxFunction<TestClass<*>>> = listOf(KxFunction<TestClass<*>>(
            name = "",
            type = KxType(
                    base = TestClass::class.kxReflect,
                    nullable = false,
                    typeParameters = listOf(KxTypeProjection.STAR),
                    annotations = listOf()
            ),
            arguments = listOf(KxArgument(
                    name = "name",
                    type = KxType(
                            base = String::class.kxReflect,
                            nullable = false,
                            typeParameters = listOf(),
                            annotations = listOf()
                    ),
                    annotations = listOf()
            ), KxArgument(
                    name = "description",
                    type = KxType(
                            base = String::class.kxReflect,
                            nullable = false,
                            typeParameters = listOf(),
                            annotations = listOf()
                    ),
                    annotations = listOf()
            ), KxArgument(
                    name = "nestMaybe",
                    type = KxType(
                            base = TestClass::class.kxReflect,
                            nullable = true,
                            typeParameters = listOf(KxTypeProjection.STAR),
                            annotations = listOf()
                    ),
                    annotations = listOf()
            ), KxArgument(
                    name = "normalParam",
                    type = KxType(
                            base = Int::class.kxReflect,
                            nullable = false,
                            typeParameters = listOf(),
                            annotations = listOf()
                    ),
                    annotations = listOf()
            )),
            call = { TestClass<Any?>(it[0] as String, it[1] as String, it[2] as TestClass<*>?, it[3] as Int) },
            annotations = listOf()
    ))
    override val annotations: List<KxAnnotation> = listOf(KxAnnotation(
            name = "Suppress",
            arguments = listOf()
    ))
}

object TestInterfaceReflection : KxClass<TestInterface> {


    override val simpleName: String = "TestInterface"
    override val qualifiedName: String = "com.lightningkite.kotlinx.reflect.properties.gradle.test.TestInterface"
    override val values: Map<String, KxValue<TestInterface, *>> = mapOf()
    override val variables: Map<String, KxVariable<TestInterface, *>> = mapOf()
    override val functions: List<KxFunction<*>> = listOf()
    override val constructors: List<KxFunction<TestInterface>> = listOf()
    override val annotations: List<KxAnnotation> = listOf()
}