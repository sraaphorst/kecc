package mpz

import it.unich.jgmp.MPZ
import it.unich.jgmp.RandState
import it.unich.jgmp.MPZ.PrimalityStatus
import java.rmi.UnexpectedException
import java.util.*
import kotlin.math.absoluteValue

// TODO: Factor randState out of here and into parameters for FP usage.
private fun randomMPZ(max: MPZ): MPZ {
    val randState = RandState.randinitMt()
    return MPZ.urandomm(randState, max)
}

// ***************
// *** HELPERS ***
// ***************
fun <T> Optional<T>.toKotlinNullable(): T? = orElse(null)


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

operator fun MPZ.times(a: Int): MPZ =
    mul(a.toLong())

operator fun Int.times(mpz: MPZ): MPZ =
    mpz.mul(this.toLong())

operator fun MPZ.times(a: Long): MPZ =
    mul(a)

operator fun Long.times(mpz: MPZ): MPZ =
    mpz.mul(this)


operator fun MPZ.plus(value: Int): MPZ =
    if (value < 0) subUi(value.toLong().absoluteValue)
    else addUi(value.toLong())

operator fun MPZ.plus(value: Long): MPZ =
    if (value < 0) subUi(value.absoluteValue)
    else addUi(value)

operator fun MPZ.minus(value: Long): MPZ =
    if (value < 0) addUi(value.absoluteValue)
    else subUi(value)

operator fun MPZ.div(value: Int): MPZ =
    this / value.toLong()

operator fun MPZ.div(value: Long): MPZ =
    if (value == 0L) throw ArithmeticException("Cannot divide $this by 0.")
    else div(value)


// *************************
// *** FINITE FIELDS Z_p ***
// *************************
class Zn(val modulus: MPZ) {
    // Equivalent to 0 until modulus.
    private val modRange = MPZ_ZERO.rangeUntil(modulus)

    init {
        require(modulus > MPZ_ZERO) { "Modulus must be greater than 0" }
        val primalityStatus = modulus.isProbabPrime(25)
        require(primalityStatus == PrimalityStatus.PRIME || primalityStatus == PrimalityStatus.PROBABLY_PRIME)
    }

    inner class EZn(val value: MPZ, private val ring: Zn = this) {
        init {
            require(value in modRange) { "Value must be in the range [0, modulus)" }
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
            val otherInv = EZn(other.value.invert(modulus).toKotlinNullable() ?:
                throw ArithmeticException("$this has no multiplicative inverse."), ring)
            return perform(MPZ::mul, otherInv)
        }

        operator fun unaryMinus(): EZn =
            EZn(value.neg().mod(modulus), ring)

        val invert: EZn? by lazy {
            value.invert(modulus).toKotlinNullable()?.let { EZn(it, ring) }
        }

        fun pow(n: MPZ): EZn =
            EZn(value.powm(n, modulus), ring)

        fun pow(n: Long): EZn {
            if (n == 0L)
                return EZn(MPZ_ONE, ring)

            // If n < 0, we must invert; otherwise we just proceed.
            val (a, np) = when {
                n < 0 -> invert?.let {Pair(invert!!.value, -n) } ?: throw ArithmeticException("$value has no inverse (mod $modulus).")
                else -> Pair(value, n)
            }
            return EZn(a.powmUi(np, modulus), ring)
        }

        // Calculate the Legendre symbol, (a/p):
        // If a non-residue class, exit immediately.
        val legendre: Legendre by lazy {
            Legendre.fromValue(value.legendre(modulus))
        }

        // Calculate the square root mod the modulus.
        val sqrt: EZn? by lazy {
            // Must be a quadratic residue to have an inverse: if not, terminate.
            if (legendre != Legendre.RESIDUE)
                return@lazy null

            // Check if p = 3 (mod 4), this is an easy computation, namely:
            // a^((p + 1)/4) (mod p).
            if (modulus.tstbit(0) == 1 && modulus.tstbit(1) == 1)
                return@lazy pow((modulus + 1).divexact(4.toMPZ()))

            // Otherwise, we must use Tonelli and Shanks.
            // Initialize q to n - 1 (even), find # of zeros on right in binary representation, and eliminate them.
            val mm1 = modulus - 1
            val e = mm1.scan1(0)
            val q = mm1.tdivq2Exp(e)

            // Find a generator. Randomly search for a non-residue.
            tailrec fun findGenerator(n: EZn = EZn(MPZ_ONE, ring)): EZn =
                if (n.legendre != Legendre.NOT_RESIDUE)
                    findGenerator(EZn(randomMPZ(modulus), ring))
                else n
            val generator = findGenerator()

            // Initialize the working components.
            // y = n^q, where q is an MPZ, so the power is calculated with the modulus.
            val y = generator.pow(q)
            var r = e

            // x = a^{(q-1)/2} as q-1 should be exactly divisible by 2 now.
            var x = pow((q - 1) / 2)
            var b = this * x * x
            x = this * x

            // Loop on algorithm until finished or failure.
            // Terminate when b == 1.
            while (b.value != MPZ_ONE) {
                var m = 1L
                var t1 = b
                while (m < r) {
                    t1 *= t1
                    if (t1.value == MPZ_ONE)
                        break
                    ++m
                }

                // This should never happen as a is a quadratic residue.
                if (r == m)
                    throw UnexpectedException("Not a quadratic residue: $this")

                val t = y.pow(1L shl (r -m - 1).toInt())
                r = m
                x *= t
                b *= y
            }

            return@lazy x
        }

        override fun toString(): String = "$value (mod $modulus}"
    }
}
