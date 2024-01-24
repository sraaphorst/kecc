package mpz

import it.unich.jgmp.MPZ
import it.unich.jgmp.RandState
import it.unich.jgmp.MPZ.PrimalityStatus
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

fun Long.pow(exponent: Int): Long = when (exponent) {
        0 -> 1L
        1 -> this
        else -> {
            val halfPower = pow(exponent / 2)
            if (exponent % 2 == 0) halfPower * halfPower
            else this * halfPower * halfPower
        }
    }

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

    val EZN_ONE = EZn(MPZ_ONE, this)

    init {
        require(modulus > MPZ_ZERO) { "Modulus must be greater than 0" }
        val primalityStatus = modulus.isProbabPrime(25)
        require(primalityStatus == PrimalityStatus.PRIME || primalityStatus == PrimalityStatus.PROBABLY_PRIME)
    }

    inner class EZn(val value: MPZ, val ring: Zn = this) {
        init {
            require(value in modRange) { "Value must be in the range [0, modulus)" }
        }

        private fun perform(op: (MPZ, MPZ, MPZ) -> MPZ, other: MPZ): EZn =
            EZn(op(value, other, modulus), ring)

        private fun perform(op: (MPZ, MPZ) -> MPZ, other: EZn): EZn {
            require(ring == other.ring) { "Attempting to perform operation between $this and $other." }
            return EZn(op(value, other.value).mod(modulus), ring)
        }

        private fun perform(op: (MPZ) -> MPZ): EZn =
            EZn(op(value).mod(modulus), ring)

        operator fun plus(other: EZn): EZn = perform(MPZ::add, other)
        operator fun minus(other: EZn): EZn = perform(MPZ::sub, other)
        operator fun unaryMinus(): EZn = perform(MPZ::neg)
        operator fun times(other: EZn): EZn = perform(MPZ::mul, other)

        operator fun div(other: EZn): EZn =
            other.invert?.let { perform(MPZ::mul, it) }
                ?: throw ArithmeticException("$this has no multiplicative inverse.")


        val invert: EZn? by lazy {
            value.invert(modulus).toKotlinNullable()?.let { EZn(it, ring) }
        }

        fun pow(n: MPZ): EZn = perform(MPZ::powm, n)

        fun pow(n: Long): EZn = when {
            n == 0L -> EZN_ONE
            n < 0 -> invert?.pow(-n) ?: throw ArithmeticException("$value has no inverse (mod $modulus).")
            else -> EZn(value.powmUi(n, modulus), ring)
        }

        // Calculate the Legendre symbol, (a/p):
        // If a non-residue class, exit immediately.
        val legendre: Legendre by lazy {
            Legendre.fromValue(value.legendre(modulus))
        }

        val sqrt: EZn? by lazy {
            when {
                legendre != Legendre.RESIDUE -> null
                modulus.isPMod4() -> pow((modulus + 1).divexactUi(4))
                else -> computeSqrtTonelliShanks()
            }
        }

        private fun MPZ.isPMod4() = tstbit(0) == 1 && tstbit(1) == 1

        private fun computeSqrtTonelliShanks(): EZn? {
            val (q, e) = decomposeModulus()
            val generator = findGenerator()
            val y = generator.pow(q)

            val xInitial = pow((q - 1) / 2)
            val bInitial = this * xInitial * xInitial
            val xModified = this * xInitial

            fun aux(r: Long = e, x: EZn = xModified, b: EZn = bInitial): EZn {
                if (b.value == MPZ_ONE) return x

                val m = findM(b, r)
                val t = y.pow(1L shl (r - m - 1).toInt())
                return aux(m, x * t, b * t.pow(2))
            }

            return aux()
        }

        private fun decomposeModulus(): Pair<MPZ, Long> {
            val mm1 = modulus - 1
            val e = mm1.scan1(0)
            val q = mm1.tdivq2Exp(e)
            return Pair(q, e)
        }

        // TODO: MAKE FUNCTIONAL
        private tailrec fun findGenerator(n: EZn = EZN_ONE): EZn =
            if (n.legendre == Legendre.NOT_RESIDUE) n else findGenerator(EZn(randomMPZ(modulus), ring))

        private tailrec fun findM(b: EZn, r: Long, m: Long = 1): Long {
            val t1 = b.pow(2L.pow(m.toInt()))
            return if (t1.value == MPZ_ONE) m else findM(b, r, m + 1)
        }

        override fun toString(): String = "$value (mod $modulus}"
    }
}
