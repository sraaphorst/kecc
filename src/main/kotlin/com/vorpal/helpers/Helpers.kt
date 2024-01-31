package com.vorpal.helpers

import java.util.Optional

fun <T> Optional<T>.toKotlinNullable(): T? = orElse(null)

fun Long.pow(exponent: Long): Long = when (exponent) {
    0L -> 1L
    1L -> this
    else -> {
        val halfPower = pow(exponent / 2)
        if (exponent % 2 == 0L) halfPower * halfPower
        else this * halfPower * halfPower
    }
}
