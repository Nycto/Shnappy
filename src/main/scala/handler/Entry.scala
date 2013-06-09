package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy._

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
    private val renderer = new Renderer

    // Attempt to render this as a slug
    delegate( new SlugHandler )

    // Error handler
    error( (request, response) => {
        case _: SiteEntry.NotFound => {
            renderer.renderPage( renderer("404") ).map {
                html => response.notFound.html( html ).done
            }
        }

        case _: Throwable => renderer.renderPage( renderer("500") ).map {
            html => response.notFound.html( html ).done
        }
    })

}

