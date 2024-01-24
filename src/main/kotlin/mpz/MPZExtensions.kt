package mpz

import it.unich.jgmp.MPZ
import it.unich.jgmp.MPZ.PrimalityStatus
import kotlin.math.absoluteValue

// *****************
// *** CONSTANTS ***
// *****************

val MPZ_ZERO: MPZ = MPZ(0L)

// ******************
// *** CONVERTERS ***
// ******************

fun Int.toMPZ(): MPZ =
    MPZ(this.toLong())

fun Long.toMPZ(): MPZ =
    MPZ(this)

fun String.toMPZ(): MPZ =
    MPZ(this)

// *********************
// *** MPZ OVERRIDES ***
// *********************

operator fun MPZ.times(a: Int): MPZ =
    mul(a.toLong())

operator fun Int.times(mpz: MPZ): MPZ =
    mpz.mul(this.toLong())

operator fun MPZ.times(a: Long): MPZ =
    mul(a)

operator fun Long.times(mpz: MPZ): MPZ =
    mpz.mul(this)


operator fun MPZ.plus(value: Long): MPZ =
    if (value < 0) subUi(value.absoluteValue)
    else addUi(value)

operator fun MPZ.minus(value: Long): MPZ =
    if (value < 0) addUi(value.absoluteValue)
    else subUi(value)

// *************************
// *** FINITE FIELDS Z_p ***
// *************************
class Zn(val modulus: MPZ) {
    // Equivalent to 0 until modulus.
    val mod_range = MPZ_ZERO..(modulus - 1)

    init {
        require(modulus > MPZ_ZERO) { "Modulus must be greater than 0" }
        val primalityStatus = modulus.isProbabPrime(25)
        require(primalityStatus == PrimalityStatus.PRIME || primalityStatus == PrimalityStatus.PROBABLY_PRIME)
    }

    fun pull(value: MPZ): EZn =
        EZn(value, this)

    inner class EZn(val value: MPZ, private val ring: Zn) {
        init {
            require(value in mod_range) { "Value must be in the range [0, modulus)" }
        }

        private fun perform(op: (MPZ, MPZ) -> MPZ, other: EZn): EZn {
            require(ring == other.ring){ "Attempting to perform operation between $this and $other." }
            return EZn(op(value, other.value).mod(modulus), ring)
        }

        operator fun plus(other: EZn): EZn =
            perform(MPZ::add, other)

        operator fun minus(other: EZn): EZn =
            perform(MPZ::sub, other)

        operator fun times(other: EZn): EZn =
            perform(MPZ::mul, other)

        operator fun div(other: EZn): EZn {
            val otherInv = EZn(other.value.invert(modulus).orElse(null) ?:
                throw ArithmeticException("$this has no multiplicative inverse."), ring)
            return perform(MPZ::mul, otherInv)
        }

        override fun toString(): String = "$value (mod $modulus}"
    }
}
