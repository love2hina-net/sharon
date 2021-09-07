package net.love2hina.kotlin.sharon

import com.github.javaparser.utils.CodeGenerationUtils
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

internal object TestUtils {

    val pathProjectRoot = CodeGenerationUtils.mavenModuleRoot(TestUtils::class.java).resolve("../..").normalize()

}

internal fun setProperty(target: Any, cls: KClass<*>, name: String, value: Any) {
    val property = cls.declaredMemberProperties
        .filterIsInstance<KMutableProperty<*>>()
        .first { it.name == name }

    property.setter.isAccessible = true
    property.setter.call(target, value)
}

internal inline fun <reified T: Any> setProperty(target: T, name: String, value: Any) {
    setProperty(target, T::class, name, value)
}

internal fun getProperty(target: Any, cls: KClass<*>, name: String): Any? {
    val property = cls.declaredMemberProperties
        .filterIsInstance<KMutableProperty<*>>()
        .first { it.name == name }

    property.getter.isAccessible = true
    return property.getter.call(target)
}

internal inline fun <reified T: Any> getProperty(target: T, name: String): Any? {
    return getProperty(target, T::class, name)
}
