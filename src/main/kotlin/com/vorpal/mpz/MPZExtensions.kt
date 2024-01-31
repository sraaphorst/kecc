package com.vorpal.mpz

import it.unich.jgmp.MPZ
import it.unich.jgmp.RandState
import com.vorpal.services.RandService
import kotlin.math.absoluteValue


fun randomMPZ(max: MPZ, randState: RandState = RandService.randState): MPZ =
    MPZ.urandomm(randState, max)

// *****************
// *** CONSTANTS ***
// *****************

val MPZ_ZERO: MPZ = MPZ(0L)
val MPZ_ONE: MPZ = MPZ(1L)

enum class Legendre(val value: Int) {
    DIVIDES(0),
    NOT_RESIDUE(-1),
    RESIDUE(1);

    companion object {
        fun fromValue(value: Int): Legendre =
            entries.firstOrNull { it.value == value }
                ?:throw IllegalArgumentException("Invalid Legendre value: $value.")
    }
}

// ******************
// *** CONVERTERS ***
// ******************

fun MPZ(i: Int): MPZ =
    MPZ(i.toLong())

fun Int.toMPZ(): MPZ =
    MPZ(this.toLong())

fun Long.toMPZ(): MPZ =
    MPZ(this)

fun String.toMPZ(): MPZ =
    MPZ(this)

// *********************
// *** MPZ OVERRIDES ***
// *********************

operator fun MPZ.unaryMinus(): MPZ =
    neg()

operator fun Int.plus(mpz: MPZ): MPZ =
    this.toLong() + mpz

operator fun MPZ.plus(value: Int): MPZ =
    this + value.toLong()

operator fun Long.plus(mpz: MPZ): MPZ =
    if (this < 0) mpz.subUi(absoluteValue)
    else mpz.addUi(this)

operator fun MPZ.plus(value: Long): MPZ =
    if (value < 0) subUi(value.absoluteValue)
    else addUi(value)

operator fun Int.minus(mpz: MPZ): MPZ =
    this.toLong() - mpz

operator fun MPZ.minus(value: Int): MPZ =
    this - value.toLong()

operator fun Long.minus(mpz: MPZ): MPZ =
    // (-x) - mpz = - (mpz + x)
    if (this < 0) mpz.addUi(this.absoluteValue).neg()
    else mpz.subUi(this.absoluteValue).neg()

operator fun MPZ.minus(value: Long): MPZ =
    if (value < 0) addUi(value.absoluteValue)
    else subUi(value)

operator fun Int.times(mpz: MPZ): MPZ =
    this.toLong() * mpz

operator fun MPZ.times(a: Int): MPZ =
    this * a.toLong()

operator fun Long.times(mpz: MPZ): MPZ =
    mpz.mul(this)

operator fun MPZ.times(a: Long): MPZ =
    mul(a)

operator fun MPZ.div(value: Int): MPZ =
    this / value.toLong()

operator fun MPZ.div(value: Long): MPZ =
    if (value == 0L) throw ArithmeticException("Cannot divide $this by 0.")
    else divexactUi(value)
