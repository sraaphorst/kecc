package com.vorpal.fields

import com.vorpal.mpz.LargePrimeArbitrary
import com.vorpal.mpz.Legendre
import com.vorpal.mpz.MPZ_ZERO
import it.unich.jgmp.MPZ
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.filter
import io.kotest.property.checkAll
import it.unich.jgmp.MPZ.PrimalityStatus
import com.vorpal.services.RandService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class ZnTest: StringSpec({
    val primeFlags = setOf(PrimalityStatus.PRIME, PrimalityStatus.PROBABLY_PRIME)
    val primeGenerator = LargePrimeArbitrary.create()
    val randState = RandService.randState

    "Primality testing" {
        checkAll(primeGenerator) { prime ->
            assertTrue { prime.isProbabPrime(15) in primeFlags }
        }
    }

    "Inverse testing" {
        checkAll(primeGenerator) { prime ->
            val ring = Zn(prime)
            val elemArb = arbitrary {_ -> ring.EZn(MPZ.urandomm(randState, prime)) }.filter { it.value > MPZ_ZERO }

            checkAll(elemArb) { elem ->
                // All nonzero elements in F_p are by definition invertible.
                assertNotNull(elem.invert)
                assertEquals(ring.EZN_ONE, elem.invert!! * elem)
            }
        }
    }

    "sqrt testing" {
        checkAll(primeGenerator) { prime ->
            val ring = Zn(prime)
            val elemArb = arbitrary { _ -> ring.EZn(MPZ.urandomm(randState, prime)) }
                .filter { it.value > MPZ_ZERO }
                .filter { it.legendre == Legendre.RESIDUE }

            checkAll(elemArb) { elem ->
                assertNotNull(elem.sqrt)
                assertEquals(elem, elem.sqrt!! * elem.sqrt!!)
            }
        }
    }
})
