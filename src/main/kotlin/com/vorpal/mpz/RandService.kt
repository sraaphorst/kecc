package com.vorpal.mpz

import it.unich.jgmp.RandState

object RandService {
    val randState: RandState by lazy {
        RandState.randinitMt()
    }
}
