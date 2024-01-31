import it.unich.jgmp.MPZ
import com.vorpal.mpz.*
import com.vorpal.fields.Zn


fun main() {
    println("Hello World!")
//    val n = MPZ("44444666666888888889999999997")
//    val ring = Zn(n)
//    val a = ring.EZn(1000.toMPZ())
//    val b = ring.EZn(2000.toMPZ())
//
//    // Using mod functions.
//    // md = a / b (mod n)
//    val md = a / b
//    println(md)
//
//    // We want to reverse the operation:
//    // a = md * b (mod n)
//    println(b * md)
//
//    val maxBound = 23L
//    println("Calculating square roots mod $maxBound")
//    val ring3 = Zn(maxBound.toMPZ())
//    (1 until maxBound).map { it.toMPZ() }.forEach {
//        val elem = ring3.EZn(it)
//        if (elem.sqrt != null) {
//            val s = elem.sqrt!!
//            println("$elem -> ${elem.sqrt} -> ${s.pow(2)} = ${s * s}")
//        }
//    }

//    val v = MPZ("25834999554458755255")
//    val m = MPZ("69256690406912238301")
    val v = MPZ("10459268535876674306")
    val m = MPZ("19963065620013555197")
    val s = MPZ("359242579880426") // as confirmed by C++ program
    val ring = Zn(m)
    val elem = ring.EZn(v)
    val sqrtElem = ring.EZn(s)
    println("Legendre of $elem is ${elem.legendre}")
    println("Inverse is ${elem.invert}")
    println("Product is ${elem.invert!! * elem}")
    println("Calculating square root...")
    println("Should be ${sqrtElem}: ${sqrtElem * sqrtElem}")
    println("Sqrt is ${elem.sqrt}")
    println("Squared is ${elem.sqrt!! * elem.sqrt!!}")
}
