package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.roundeights.skene._
import com.roundeights.attempt._
import com.roundeights.shnappy._

/**
 * Handles requests for site specific content
 */
class SiteHandler (
    private val env: Env,
    private val ctx: Context
) extends Skene {

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
            rendered <- recover.fromFuture( page.render(ctx.renderer) )

            // The rendered page content
            html <- recover.fromFuture( ctx.renderer.renderPage( rendered ) )
        } {
            response.ok.html( html ).done
        }
    }

    // Wire up a handler for the favicon
    get("/favicon.ico")( (request, response) => ctx.favicon match {
        case Some(path) => env.assets.serve(path, request, response)
        case None => env.assets.serve(
            "favicon/%s.ico".format(ctx.theme), request, response
        )
    })

    // Render the index
    index( (recover, request, response) => {
        showPage(recover, response, ctx.data.getIndex )
    })

    // Returns a specific page
    get("/:page") { (recover, request, response) => {
        showPage(recover, response,
            ctx.data.getPage( request.params("page") )
        )
    }}

    // Error handler
    error( (recover, request, response) => {

        case _: SiteEntry.NotFound => recover.fromFuture(
            ctx.renderer.renderPage( ctx.renderer("404") ).map {
                html => response.notFound.html( html ).done
            }
        )

        case err: Throwable => {
            TryTo(
                ctx.renderer.renderPage( ctx.renderer("500") ).map( html => {
                    err.printStackTrace
                    response.serverError.html( html ).done
                })
            ) onFailMatch {
                case secondErr: Throwable => {
                    secondErr.printStackTrace
                    recover.orRethrow(err)
                }
            }
        }
    })

}


