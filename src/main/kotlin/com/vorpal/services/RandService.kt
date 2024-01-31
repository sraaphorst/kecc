package com.vorpal.services

import it.unich.jgmp.RandState

object RandService {
    val randState: RandState by lazy {
        RandState.randinitMt()
    }
}
