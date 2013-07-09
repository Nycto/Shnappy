package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject
import com.roundeights.shnappy._

/**
 * Admin handlers
 */
class AdminHandler( env: Env, data: AdminData ) extends Skene {

    // Template builder
    val template = new Templater( env )

    /** A registry of Prereq providers */
    val prereq = Registry()
        .register[Auth]( new AuthProvider(new Session(env.secretKey), data) )

    // Admin pages MUST be https in production
    protected def requireSecure( that: Skene,
        callback: (Response, String) => Unit
    ): Unit = {
        if ( env.httpsOnlyAdmin ) {
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

    /** Generates an HTML error message for the given response */
    private def htmlError ( resp: Response, message: String ): Unit = {
        resp.html( template( "admin/page",
            "title" -> "Error",
            "content" -> template("admin/error", "message" -> message)
        ) ).done
    }

    /** Generates a json error message for the given response */
    private def jsonError ( resp: Response, message: String ): Unit
        = resp.json( nObject("error" -> message).toString ).done


    // HTML handlers
    delegate( new Skene {
        requireSecure( this, htmlError(_, _) )
        handleErrors( this, htmlError(_, _) )
    })

    // API handlers
    delegate( new Skene {
        requireSecure( this, jsonError(_, _) )
        handleErrors( this, jsonError(_, _) )

        delegate( new PageApiHandler(prereq) )
    })

    // 404
    default((req: Request, resp: Response) => {
        htmlError(resp, "404 Not Found")
    })

}


