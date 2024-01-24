package mpz

import java.io.File
import kotlin.random.Random

import it.unich.jgmp.MPZ

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary

object LargePrimeArbitrary {
    private val primes: List<MPZ> by lazy {
        val resourceURL = this::class.java.classLoader.getResource("large_primes.txt") ?:
            throw RuntimeException("Could not read large primes.")
        val file = File(resourceURL.file)
        file.readLines().map(::MPZ)
    }

    fun create(): Arb<MPZ> = arbitrary {
        primes[Random.nextInt(primes.size)]
    }
}

