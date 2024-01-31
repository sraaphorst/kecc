package com.vorpal.fields

import com.vorpal.helpers.toKotlinNullable
import com.vorpal.mpz.*
import com.vorpal.services.RandService
import it.unich.jgmp.MPZ
import it.unich.jgmp.RandState

// *************************
// *** FINITE FIELDS Z_p ***
// *************************
class Zn(val modulus: MPZ) {
    // Service locator pattern for the RandState.
    private val randState = RandService.randState

    // Equivalent to 0 until modulus.
    private val modRange = MPZ_ZERO.rangeUntil(modulus)

    val EZN_ONE = EZn(MPZ_ONE, this)

    init {
        require(modulus > MPZ_ZERO) { "Modulus must be greater than 0" }
        val primalityStatus = modulus.isProbabPrime(25)
        require(primalityStatus == MPZ.PrimalityStatus.PRIME || primalityStatus == MPZ.PrimalityStatus.PROBABLY_PRIME)
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
            n < 0 -> {
                if (n == -n)
                    invert?.pow(-(n.toMPZ())) ?: throw ArithmeticException("$value has no inverse (mod $modulus).")
                else
                    invert?.pow(-n) ?: throw ArithmeticException("$value has no inverse (mod $modulus).")
            }
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
                else -> computeSqrtTonelliShanks(randState)
            }
        }

        private fun MPZ.isPMod4() = tstbit(0) == 1 && tstbit(1) == 1

        private tailrec fun calcM(r: Long, t: EZn, m: Long = 1L): Long {
            if (m >= r) return m
            val tNew = t * t
            if (tNew.value == MPZ_ONE) return m
            return calcM(r, tNew, m+1)
        }

        private fun decomposeModulus(): Pair<MPZ, Long> {
            val mm1 = modulus - 1
            val e = mm1.scan1(0)
            val q = mm1.tdivq2Exp(e)
            return Pair(q, e)
        }

        private fun computeSqrtTonelliShanks(randState: RandState): EZn {
            val (q, e) = decomposeModulus()
            val generator = findGenerator(randState)
            val yInitial = generator.pow(q)
            val xInitial = pow((q - 1) / 2)
            val bInitial = this * xInitial * xInitial
            val xModified = this * xInitial

            fun recursiveSqrtTonelliShanks(x: EZn, y: EZn, r: Long, b: EZn): EZn {
                if (b.value == MPZ_ONE) return x

                val m = calcM(r, b)
                if (r == m) throw RuntimeException("Unexpected error: $this is not behaving like a quadratic residue.")

                val shiftBy = r - m - 1
                val shift = (1 shl shiftBy.toInt()).toLong()
                val t = y.pow(shift)

                val newX = x * t
                val newY = t * t
                val newR = m
                val newB = b * newY

                return recursiveSqrtTonelliShanks(newX, newY, newR, newB)
            }

            return recursiveSqrtTonelliShanks(xModified, yInitial, e, bInitial)
        }

        private tailrec fun findGenerator(randState: RandState, n: EZn = EZN_ONE): EZn = when (n.legendre) {
            Legendre.NOT_RESIDUE -> n
            else                 -> findGenerator(randState, EZn(randomMPZ(modulus, randState), ring))
        }

        override operator fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as EZn
            if (value != other.value) return false
            if (ring.modulus != other.ring.modulus) return false
            return true
        }

        override fun hashCode(): Int =
            31 * value.hashCode() + ring.modulus.hashCode()

        override fun toString(): String = "$value (mod $modulus)"
    }
}
