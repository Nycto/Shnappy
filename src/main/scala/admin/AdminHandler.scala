package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject
import com.roundeights.shnappy._

/**
 * Admin handlers
 */
class AdminHandler( env: Env, data: AdminData ) extends Skene {

    /** A registry of Prereq providers */
    val prereq = Registry()
        .register[Auth]( new AuthProvider(new Session(env.secretKey), data) )
        .register[BodyData]( new BodyDataProvider )


    // Admin pages MUST be https in production
    protected def requireSecure( that: Skene,
        callback: (Response, String) => Unit
    ): Unit = {
        if ( !env.adminDevMode ) {
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


    // API handlers
    request("/admin/api/**")(new Skene {

        /** Generates a json error message for the given response */
        def error ( resp: Response, message: String ): Unit
            = resp.json( nObject("error" -> message).toString ).done

        requireSecure( this, error(_, _) )
        handleErrors( this, error(_, _) )
        default((req: Request, resp: Response) => error(resp, "404 Not Found"))

        delegate( new PageApiHandler(prereq) )
        delegate( new AuthApiHandler(prereq) )
    })


    // HTML handlers
    delegate( new Skene {

        // Template builder
        val template = new Templater( env )

        /** Generates an HTML error message for the given response */
        def error ( resp: Response, message: String ): Unit = {
            resp.html( template( "admin/page",
                "title" -> "Error",
                "content" -> template("admin/error", "message" -> message)
            ) ).done
        }

        requireSecure( this, error(_, _) )
        handleErrors( this, error(_, _) )
        default((req: Request, resp: Response) => error(resp, "404 Not Found"))
    })

}


