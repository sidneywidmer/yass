package ch.yass.core.helper

import java.util.EnumMap

fun <K : Enum<K>, V> Map<K, V>.toEnumMap(): EnumMap<K, V> = EnumMap(this)

fun <K : Enum<K>, V> Iterable<K>.associateWithToEnum(valueSelector: (K) -> V): EnumMap<K, V> =
    EnumMap(associateWith(valueSelector))

fun <K : Enum<K>, V, R> EnumMap<K, V>.mapValuesToEnum(transform: (Map.Entry<K, V>) -> R): EnumMap<K, R> =
    EnumMap(mapValues { transform(it) })