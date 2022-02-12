package net.dankito.banking.fints.extensions

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail


fun assertEquals(expected: Any?, actual: Any?) {
  assertEquals(expected, actual, "'$actual' should equal expected '$expected'")
}

fun assertFalse(actual: Boolean) {
  kotlin.test.assertFalse(actual, "Value should be false but isn't")
}

fun assertTrue(actual: Boolean) {
  assertTrue(actual, "Value should be true but isn't")
}

@OptIn(ExperimentalContracts::class)
fun assertNotNull(actual: Any?) {
  contract { returns() implies (actual != null) }
  assertNotNull(actual, "Value should not be null but is")
}

fun assertNull(actual: Any?) {
  kotlin.test.assertNull(actual, "'$actual' is expected to be null")
}

fun assertEmpty(string: String) {
  assertTrue(string.isEmpty())
}

fun assertContains(string: String, vararg args: String) {
  args.forEach { arg ->
    kotlin.test.assertContains(string, arg)
  }
}


fun assertSize(size: Int, collection: Collection<*>) {
  assertEquals(size, collection.size, "Collection should have size $size, but has ${collection.size} elements")
}

fun assertEmpty(collection: Collection<*>) {
  assertTrue(collection.isEmpty())
}

fun <T : Any?> assertContainsExactly(collection: Collection<T>, vararg items: T) {
  assertEquals(collection.size, items.size, "Size of collection is ${collection.size}, but that of expected arguments is ${items.size}")

  assertContains(collection, *items)
}

fun <T : Any?> assertContains(collection: Collection<T>, vararg items: T) {
  items.forEach { item ->
    kotlin.test.assertContains(collection, item)
  }
}


inline fun <reified T : Throwable> assertThrows(action: () -> Unit) {
  try {
    action()
    fail("action() didn't throw any exception. Expected was ${T::class.qualifiedName}")
  } catch (throwable: Throwable) {
    assertTrue(throwable is T)
  }
}