package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.roundeights.skene._
import com.roundeights.attempt._
import com.roundeights.shnappy._
import com.roundeights.shnappy.component.Parser
import com.roundeights.shnappy.admin.AdminHandler

/** @see SiteEntry */
object SiteEntry {

    /** Thrown when a page doesn't exist */
    class NotFound extends Exception
}

/**
 * The primary entry point for site requests
 */
class SiteEntry ( env: Env ) extends Skene {

    /** Data access */
    private val data = Data( env, Parser.parser )

    /** A shared renderer */
    private val renderer = new Renderer( env, data )

    // Attempt to load any support endpoints
    delegate( new UtilEntry(env) )

    // Handle Admin requests
    request("/admin/**")( new AdminHandler(env, data.admin) )

    // Attempt to render this as a slug
    delegate( new SlugHandler(data, renderer) )

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

