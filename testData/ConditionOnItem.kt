package com.lightningkite.kotlinx.server

import com.lightningkite.kotlinx.reflection.ExternalReflection
import com.lightningkite.kotlinx.reflection.KxField

@ExternalReflection
sealed class ConditionOnItem<in T: Any> {

    abstract operator fun invoke(item: T): Boolean

    @ExternalReflection
    interface OnField<T: Any, V> {
        val field: KxField<T, V>
    }

    @ExternalReflection
    class Never<T: Any> : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = false
    }

    @ExternalReflection
    class Always<T: Any> : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = true
    }

    @ExternalReflection
    class And<T: Any>(vararg val conditions: ConditionOnItem<T>) : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = conditions.all { it(item) }
    }

    @ExternalReflection
    class Or<T: Any>(vararg val conditions: ConditionOnItem<T>) : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = conditions.any { it(item) }
    }

    @ExternalReflection
    class Not<T: Any>(val condition: ConditionOnItem<T>) : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = !condition(item)
    }

    @ExternalReflection
    class Equal<T: Any, V>(override val field: KxField<T, V>, val value: V) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) == value
    }

    @ExternalReflection
    class EqualToOne<T: Any, V>(override val field: KxField<T, V>, val values: Collection<V>) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) in values
    }

    @ExternalReflection
    class NotEqual<T: Any, V>(override val field: KxField<T, V>, val value: V) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) != value
    }

    @ExternalReflection
    class LessThan<T: Any, V : Comparable<V>>(override val field: KxField<T, V>, val value: V) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) < value
    }

    @ExternalReflection
    class GreaterThan<T: Any, V : Comparable<V>>(override val field: KxField<T, V>, val value: V) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) > value
    }

    @ExternalReflection
    class LessThanOrEqual<T: Any, V : Comparable<V>>(override val field: KxField<T, V>, val value: V) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) <= value
    }

    @ExternalReflection
    class GreaterThanOrEqual<T: Any, V : Comparable<V>>(override val field: KxField<T, V>, val value: V) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) >= value
    }

    @ExternalReflection
    class TextSearch<T: Any, V : CharSequence>(override val field: KxField<T, V>, val query: String) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item).contains(query)
    }

    @ExternalReflection
    class RegexTextSearch<T: Any, V : CharSequence>(override val field: KxField<T, V>, val query: String) : ConditionOnItem<T>(), OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item).contains(Regex(query))
    }
}