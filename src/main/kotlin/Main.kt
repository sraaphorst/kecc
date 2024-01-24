package org.example

import it.unich.jgmp.MPZ
import mpz.*


fun main() {
    println("Hello World!")
    val n = MPZ("44444666666888888889999999997")
    val ring = Zn(n)
    val a = ring.pull(1000.toMPZ())
    val b = ring.pull(2000.toMPZ())

    // Using mod functions.
    // md = a / b (mod n)
    val md = a / b
    println(md)

    // We want to reverse the operation:
    // a = md * b (mod n)
    println(b * md)

    val ring2 = Zn("13".toMPZ())
    val c = ring2.pull(10.toMPZ())
    val d = a * c
    println(d)
}
