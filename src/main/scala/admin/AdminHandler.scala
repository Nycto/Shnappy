package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject
import com.roundeights.shnappy._

/**
 * Admin handlers
 */
class AdminHandler(
    env: Env, data: AdminData, baseTemplate: Templater
) extends Skene {

    /** A registry of Prereq providers */
    val prereq = Registry()
        .register[Auth]( new AuthProvider(new Session(env.secretKey), data) )
        .register[BodyData]( new BodyDataProvider )

    // Template builder
    val template = baseTemplate.wrap("admin/page", "content")


    /** A base handler for admin pages */
    trait BaseAdmin extends Skene {

        /** Generates a custom error message */
        def formatErr ( resp: Response, message: String ): Unit

        /** A custom error handler */
        def customErr( resp: Response ): PartialFunction[Throwable, Unit] = {
            new PartialFunction[Throwable, Unit] {
                override def apply(v1: Throwable): Unit = {}
                override def isDefinedAt(x: Throwable): Boolean = false
            }
        }

        // Centralized error handler
        error((req, resp) => customErr(resp).orElse({
            case err: Auth.Unauthenticated
                => formatErr( resp.unauthorized, "Unauthenticated request" )
            case err: Throwable => {
                err.printStackTrace
                formatErr( resp.serverError, "Internal server error" )
            }
        }))

        // Hook in a default page handler when nothing else matches
        default( (_: Request, r: Response) => formatErr(r, "404 Not Found") )

        // Fail non-secure requests
        if ( !env.adminDevMode )
            notSecure(_ => throw new Auth.Insecure)
    }


    // API handlers
    request("/admin/api/**")(new BaseAdmin {

        /** {@inheritDoc} */
        override def formatErr ( resp: Response, message: String ): Unit = {
            resp.json( nObject(
                "status" -> "error",
                "message" -> message
            ).toString ).done
        }

        delegate( new PageApiHandler(prereq) )
        delegate( new AuthApiHandler(prereq) )
    })


    // HTML handlers
    delegate( new BaseAdmin {

        /** {@inheritDoc} */
        override def formatErr ( resp: Response, message: String ): Unit
            = resp.html( template("admin/error", "message" -> message) ).done

        /** {@inheritDoc} */
        override def customErr(resp: Response) = {
            case err: Auth.Unauthenticated
                => resp.html( template("admin/login") ).done
        }

        delegate( new PageHtmlHandler(template, prereq) )
    })

}


