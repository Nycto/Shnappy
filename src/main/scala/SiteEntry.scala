package com.roundeights.shnappy

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.attempt._

class SiteEntry extends Skene {

    /** Thrown when a page doesn't exist */
    class SlugNotFound extends Exception

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
                recover.orRethrow( new SlugNotFound )
            }

            // Render the page components
            rendered <- recover.fromFuture( page.render(renderer) )
        } {
            response.ok.html( rendered ).done
        }
    }}

}

