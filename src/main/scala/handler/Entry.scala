package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.roundeights.skene._
import com.roundeights.shnappy._
import com.roundeights.shnappy.component.Parser
import com.roundeights.shnappy.admin.AdminHandler

/** @see SiteEntry */
object SiteEntry {

    /** Thrown when a page doesn't exist */
    class NotFound extends Exception

    /** Thrown when a site does not exist in the site */
    class NoSite extends Exception
}

/**
 * The primary entry point for site requests
 */
class SiteEntry ( env: Env ) extends Skene {

    /** Data access */
    private val data = Data( env, Parser.parser )

    /** The root templating engine */
    private val templates = Templater( env )
        .handleList( "js", content => env.js.js( content:_* ) )
        .handleList( "css", content => env.css.css( content:_* ) )
        .handle( "asset", content => env.assets.url(content).getOrElse("") )

    // Attempt to load any support endpoints
    delegate( new UtilEntry(env) )

    // Handle Admin requests
    request("/admin/**")( new AdminHandler(env, data.admin, templates) )

    // Default behavior is to render this as a slug
    default( (recover: Recover, request: Request, response: Response) => {
        recover.fromFuture(
            data.forSite.map {
                case None => throw new SiteEntry.NoSite
                case Some(reqData) => new SiteHandler( env, new Context(
                    reqData, new Renderer( templates, reqData )
                )).handle( recover, request, response )
            }
        )
    })

    // Error handler
    error( (request, response) => {
        case err: SiteEntry.NoSite => {
            response.notFound.html(
                <html>
                    <head><title>404 Not Found</title></head>
                    <body><h1>404 Site Not Found</h1></body>
                </html>
            ).done
        }

        case err: Throwable => {
            err.printStackTrace
            response.serverError.html(
                <html>
                    <head><title>500 Internal Server Error</title></head>
                    <body><h1>500 Internal Server Error</h1></body>
                </html>
            ).done
        }
    })

}

