package com.roundeights.shnappy

import scala.concurrent.{Future, ExecutionContext}
import scala.collection.JavaConversions

import com.github.jknack.handlebars.io.FileTemplateLoader
import com.github.jknack.handlebars.{Handlebars, Context, Helper, Options}
import java.io.File
import java.util.{HashMap => JavaMap}


/** A renderer is used to translate data into HTML */
class Renderer ( private val env: Env, private val data: Data ) {

    /** Templating engine */
    private val engine = new Templater( env ) {

        // Add a helper to load the JavaScript
        handleList( "js", content => env.js.js( content:_* ) )

        // Add a helper to load the CSS
        handleList( "css", content => env.css.css( content:_* ) )

        // Add a helper to load the URL for a single asset
        handle( "asset", content => env.assets.url(content).getOrElse("") )
    }

    /** Renders the given component type with the given data */
    def apply ( template: String, data: (String, Any)* ): String
        = engine( template, Map(data:_*) )

    /** Renders the page level template */
    def renderPage
        ( content: String )
        ( implicit ctx: ExecutionContext )
    : Future[String] = {
        val linksFuture = data.getNavLinks
        val infoFuture = data.getSiteInfo

        linksFuture.flatMap( links => infoFuture.map( info => {
            engine.apply( "page",
                info.toMap +
                ("content" -> content) +
                ("nav" -> links.map( _.toMap ))
            )
        }))
    }

}

