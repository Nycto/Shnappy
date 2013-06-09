package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.attempt._
import com.roundeights.shnappy._

/**
 * Handles requests to miscellanious pages
 */
class SlugHandler extends Skene {

    /** A shared renderer */
    private val renderer = new Renderer

    // Returns a specific page
    get("/:page") { (recover, request, response) => {
        for {

            // Pull the page from the DB
            pageOpt: Option[Page] <- recover.fromFuture(
                Data().getPage( request.params("page") )
            )

            // Make sure the page exists
            page <- TryTo( pageOpt ) onFail {
                recover.orRethrow( new SiteEntry.NotFound )
            }

            // Render the page components
            rendered <- recover.fromFuture( page.render(renderer) )

            // The rendered page content
            html <- renderer.renderPage( rendered )
        } {
            response.ok.html( html ).done
        }
    }}

}


