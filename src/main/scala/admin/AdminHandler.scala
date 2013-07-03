package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject
import com.roundeights.shnappy.Env

/**
 * Admin handlers
 */
class AdminHandler extends Skene {

    // Admin pages MUST be https in production
    protected def requireSecure( that: Skene,
        callback: (Response, String) => Unit
    ): Unit = {
        if ( Env.env.httpsOnlyAdmin ) {
            that.notSecure((resp: Response) => callback(
                resp.badRequest, "This page is only accessible via HTTPS"
            ))
        }
    }

    // Hooks in a standard error handler
    protected def handleErrors( that: Skene,
        callback: (Response, String) => Unit
    ): Unit = {
        that.error((request, response) => {
            case err: Throwable => {
                err.printStackTrace
                callback( response.serverError, "Internal server error" )
            }
        })
    }


    // HTML handlers
    delegate( new Skene {

        /** Generates an HTML error message for the given response */
        private def error ( resp: Response, message: String ): Unit
            = resp.html(message).done

        requireSecure( this, error(_, _) )
        handleErrors( this, error(_, _) )

        delegate( new PageHandler )
    })

    // API handlers
    delegate( new Skene {

        /** Generates a json error message for the given response */
        private def error ( resp: Response, message: String ): Unit
            = resp.json( nObject("error" -> message).toString ).done

        requireSecure( this, error(_, _) )
        handleErrors( this, error(_, _) )
    })
}


