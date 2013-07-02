package com.roundeights.shnappy.admin

import com.roundeights.shnappy._
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry}

/**
 * Coordinates request prerequisites
 */
object Prereq {

    /**
     * The prerequisites
     */
    val require = Registry()
        .register[Auth](
            new AuthProvider( new Session(Env.env.secretKey), Data.admin )
        )
}


