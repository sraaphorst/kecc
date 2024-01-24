package org.example

import it.unich.jgmp.MPZ
import mpz.*


fun main() {
    println("Hello World!")
    val n = MPZ("44444666666888888889999999997")
    val ring = Zn(n)
    val a = ring.EZn(1000.toMPZ())
    val b = ring.EZn(2000.toMPZ())

    // Using mod functions.
    // md = a / b (mod n)
    val md = a / b
    println(md)

    // We want to reverse the operation:
    // a = md * b (mod n)
    println(b * md)

    val maxBound = 23L
    println("Calculating square roots mod $maxBound")
    val ring3 = Zn(maxBound.toMPZ())
    (1 until maxBound).map { it.toMPZ() }.forEach {
        val elem = ring3.EZn(it)
        if (elem.sqrt != null) {
            val s = elem.sqrt!!
            println("$elem -> ${elem.sqrt} -> ${s.pow(2)} = ${s * s}")
        }
    }
}
