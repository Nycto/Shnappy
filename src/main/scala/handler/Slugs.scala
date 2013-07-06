package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.roundeights.skene._
import com.roundeights.attempt._
import com.roundeights.shnappy._

/**
 * Handles requests to miscellanious pages
 */
class SlugHandler( data: Data, renderer: Renderer ) extends Skene {

    /** Renders the given page */
    private def showPage (
        recover: Recover, response: Response, pageFuture: Future[Option[Page]]
    ): Unit = {
        for {
            // Pull the page from the DB
            pageOpt: Option[Page] <- recover.fromFuture( pageFuture )

            // Make sure the page exists
            page <- TryTo( pageOpt ) onFail {
                recover.orRethrow( new SiteEntry.NotFound )
            }

            // Render the page components
            rendered <- recover.fromFuture( page.render(renderer) )

            // The rendered page content
            html <- recover.fromFuture( renderer.renderPage( rendered ) )
        } {
            response.ok.html( html ).done
        }
    }

    // Render the index
    index( (recover, request, response) => {
        showPage(recover, response, data.getIndex )
    })

    // Returns a specific page
    get("/:page") { (recover, request, response) => {
        showPage(recover, response, data.getPage( request.params("page") ) )
    }}

}


