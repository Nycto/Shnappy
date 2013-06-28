package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.roundeights.skene._
import com.roundeights.shnappy._
import com.roundeights.attempt._

/** @see SiteEntry */
object SiteEntry {

    /** Thrown when a page doesn't exist */
    class NotFound extends Exception
}

/**
 * The primary entry point for site requests
 */
class SiteEntry extends Skene {

    /** A shared renderer */
    private val renderer = new Renderer( Env.env, Data() )

    // Load the static assets
    delegate( Env.env.loader.handler )

    // Attempt to render this as a slug
    delegate( new SlugHandler )

    /** Recovers from a future */
    private def recover[T] ( resp: Response, future: Future[T] ): Unit = {
        TryTo( future ).onFailMatch {
            case err: Throwable => {
                err.printStackTrace
                resp.serverError.html(
                    <html>
                        <head><title>500 Internal Server Error</title></head>
                        <body><h1>500 Internal Server Error</h1></body>
                    </html>
                ).done
            }
        }
    }

    // Error handler
    error( (request, response) => {

        case _: SiteEntry.NotFound => recover( response,
            renderer.renderPage( renderer("404") ).map {
                html => response.notFound.html( html ).done
            }
        )

        case err: Throwable => {
            err.printStackTrace
            recover(
                response,
                renderer.renderPage( renderer("500") ).map {
                    html => response.notFound.html( html ).done
                }
            )
        }
    })

}

