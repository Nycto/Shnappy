package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy.Env

/**
 * Admin handlers
 */
class AdminHandler extends Skene {

    // Admin pages MUST be https in production
    if ( Env.env.httpsOnlyAdmin ) {
        notSecure((response: Response) => {
            response.badRequest.text(
                "This page is only accessible via HTTPS"
            ).done
        })
    }

    delegate( new PageHandler )
}


