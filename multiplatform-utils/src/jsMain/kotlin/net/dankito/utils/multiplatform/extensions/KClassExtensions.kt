package net.dankito.utils.multiplatform.extensions

import kotlin.reflect.KClass

/**
 * On some platforms like JavaScript [KClass.qualifiedName] is not supported.
 * On these [KClass.simpleName] is returned, on all others [KClass.qualifiedName].
 */
actual val KClass<*>.platformSpecificQualifiedName: String
  get() = this.simpleName // on JavaScript qualifiedName is not supported
    ?: "ClassNameNotFound" // should actually never occur